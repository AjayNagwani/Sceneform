package com.test.sceneformapp;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.CollisionShape;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.function.Consumer;

public class TouchController {

    private static final int TOUCH_STATUS_ZOOMING_CAMERA = 1;
    private static final int TOUCH_STATUS_ROTATING_CAMERA = 4;
    private static final int TOUCH_STATUS_MOVING_WORLD = 5;
    private static final int TOUCH_STATUS_SINGLE_TAP = 2;

    private final Scene scene;
    private int pointerCount = 0;
    private float x1 = Float.MIN_VALUE;
    private float y1 = Float.MIN_VALUE;
    private float x2 = Float.MIN_VALUE;
    private float y2 = Float.MIN_VALUE;
    private float dx1 = Float.MIN_VALUE;
    private float dy1 = Float.MIN_VALUE;
    private float dx2 = Float.MIN_VALUE;
    private float dy2 = Float.MIN_VALUE;

    private float length = Float.MIN_VALUE;
    private float previousLength = Float.MIN_VALUE;
    private float currentPress1 = Float.MIN_VALUE;
    private float currentPress2 = Float.MIN_VALUE;

    private float rotation = 0;
    private int currentSquare = Integer.MIN_VALUE;

    private boolean isOneFixedAndOneMoving = false;
    private boolean fingersAreClosing = false;
    private boolean isRotating = false;

    private boolean gestureChanged = false;
    private boolean moving = false;
    private boolean simpleTouch = false;
    private long lastActionTime;
    private int touchDelay = -2;
    private int touchStatus = -1;

    private float previousX1;
    private float previousY1;
    private float previousX2;
    private float previousY2;
    private float[] previousVector = new float[4];
    private float[] vector = new float[4];
    private float[] rotationVector = new float[4];
    private float previousRotationSquare;
    private String TAG = "TouchController.this";
    private Integer touchCounter = 0;
    private ModelRenderable redSphereRenderable;
    private Context mContext;

    public TouchController(Scene scene) {
        this.scene = scene;
    }

    public synchronized boolean onTouchEvent(MotionEvent motionEvent, TransformableNode finalNode, HitTestResult hitTestResult, Context applicationContext) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        mContext = applicationContext;
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_OUTSIDE:
                touchCounter = 0;
                // this to handle "1 simple touch"
                if (lastActionTime > SystemClock.uptimeMillis() - 250) {
                    simpleTouch = true;
                    touchStatus = TOUCH_STATUS_SINGLE_TAP;
                } else {
                    gestureChanged = true;
                    touchDelay = 0;
                    lastActionTime = SystemClock.uptimeMillis();
                    simpleTouch = false;
                }
                moving = false;
                break;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_HOVER_ENTER:
                Log.d(TAG, "Gesture changed...");
                gestureChanged = true;
                touchDelay = 0;
                lastActionTime = SystemClock.uptimeMillis();
                simpleTouch = false;
                break;
            case MotionEvent.ACTION_MOVE:
                moving = true;
                simpleTouch = false;
                touchDelay++;
                break;
            default:
                Log.w(TAG, "Unknown state: " + motionEvent.getAction());
                // gestureChanged = true;
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                touchCounter = 0;
                break;
            case MotionEvent.ACTION_DOWN:
                touchCounter = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                touchCounter = touchCounter + 1;
                break;
        }


        pointerCount = motionEvent.getPointerCount();

        if (pointerCount == 1) {
            x1 = motionEvent.getX();
            y1 = motionEvent.getY();
            if (gestureChanged) {
                Log.d(TAG, "x:" + x1 + ",y:" + y1);
                previousX1 = x1;
                previousY1 = y1;
            }
            dx1 = x1 - previousX1;
            dy1 = y1 - previousY1;
        } else if (pointerCount == 2) {
            x1 = motionEvent.getX(0);
            y1 = motionEvent.getY(0);
            x2 = motionEvent.getX(1);
            y2 = motionEvent.getY(1);
            vector[0] = x2 - x1;
            vector[1] = y2 - y1;
            vector[2] = 0;
            vector[3] = 1;
            float len = Matrix.length(vector[0], vector[1], vector[2]);
            vector[0] /= len;
            vector[1] /= len;

            if (gestureChanged) {
                previousX1 = x1;
                previousY1 = y1;
                previousX2 = x2;
                previousY2 = y2;
                System.arraycopy(vector, 0, previousVector, 0, vector.length);
            }
            dx1 = x1 - previousX1;
            dy1 = y1 - previousY1;
            dx2 = x2 - previousX2;
            dy2 = y2 - previousY2;

            rotationVector[0] = (previousVector[1] * vector[2]) - (previousVector[2] * vector[1]);
            rotationVector[1] = (previousVector[2] * vector[0]) - (previousVector[0] * vector[2]);
            rotationVector[2] = (previousVector[0] * vector[1]) - (previousVector[1] * vector[0]);
            len = Matrix.length(rotationVector[0], rotationVector[1], rotationVector[2]);
            rotationVector[0] /= len;
            rotationVector[1] /= len;
            rotationVector[2] /= len;

            previousLength = (float) Math
                    .sqrt(Math.pow(previousX2 - previousX1, 2) + Math.pow(previousY2 - previousY1, 2));
            length = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

            currentPress1 = motionEvent.getPressure(0);
            currentPress2 = motionEvent.getPressure(1);
            rotation = 0;
            rotation = TouchScreen.getRotation360(motionEvent);
            currentSquare = TouchScreen.getSquare(motionEvent);
            if (currentSquare == 1 && previousRotationSquare == 4) {
                rotation = 0;
            } else if (currentSquare == 4 && previousRotationSquare == 1) {
                rotation = 360;
            }

            // gesture detection

            isOneFixedAndOneMoving = ((dx1 + dy1) == 0) != (((dx2 + dy2) == 0));
            fingersAreClosing = !isOneFixedAndOneMoving && (Math.abs(dx1 + dx2) < 10 && Math.abs(dy1 + dy2) < 10);
            isRotating = !isOneFixedAndOneMoving && (dx1 != 0 && dy1 != 0 && dx2 != 0 && dy2 != 0)
                    && rotationVector[2] != 0;
        }

        if (pointerCount == 1 && simpleTouch) {
           // createCircle(hitTestResult, finalNode);
        }

        if (touchDelay > 1) {
            // INFO: Process gesture
            if (pointerCount == 1 && currentPress1 > 4.0f) {
                // createCircle(hitTestResult,finalNode);
            } else if (pointerCount == 1) {
                if(touchCounter<7&&motionEvent.getActionMasked()== MotionEvent.ACTION_UP){
                   createCircle(hitTestResult,finalNode);

                }
                else if (touchCounter > 7 && motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    Log.i("touched", "Move");
                    touchStatus = TOUCH_STATUS_MOVING_WORLD;
                    Camera camera = scene.getCamera();
                    //Log.i(TAG, "Translating camera (dz) '" + camera.getLocalPosition().z);

                    // Log.i("Translating", "dx : " + dx1);
                    float anglePan = (float) Math.sqrt((Math.pow(dx1, 2) + Math.pow(dy1, 2)) * (Math.PI) / 180.0);
                    Log.d("Panangle", String.valueOf(anglePan));
                    if (dx1 > 0.0 && dx1 > dy1 - dx1) {
                        Quaternion rotationDelta = new Quaternion(Vector3.up(), 3 * anglePan);
                        Quaternion localrotation = finalNode.getLocalRotation();
                        localrotation = Quaternion.multiply(localrotation, rotationDelta);
                        //  Log.e("rotation right : ", dx1 + " " + dy1);
                        finalNode.setLocalRotation(localrotation);
                    } else if (dx1 < 0.0 && dx1 < dy1 - dx1) {
                        Quaternion rotationDelta = new Quaternion(Vector3.down(), 3 * anglePan);
                        Quaternion localrotation = finalNode.getLocalRotation();
                        localrotation = Quaternion.multiply(localrotation, rotationDelta);
                        //Log.e("rotation left : ", dx1 + " " + dy1);
                        finalNode.setLocalRotation(localrotation);
                    }
                  /*  if(dy1 > 0.0 && dy1 > dx1 - dy1) {
                        Quaternion rotationDelta = new Quaternion(Vector3.right(), anglePan);
                        Quaternion localrotation = finalNode.getLocalRotation();
                        localrotation = Quaternion.multiply(localrotation, rotationDelta);
                        Log.e("rotation top : ", dx1 + " "+dy1);
                        finalNode.setLocalRotation(localrotation);
                    }
                    else
                    if(dy1 < 0.0 && dy1 < dx1 - dy1) {
                        Quaternion rotationDelta = new Quaternion(Vector3.left(), anglePan);
                        Quaternion localrotation = finalNode.getLocalRotation();
                        localrotation = Quaternion.multiply(localrotation, rotationDelta);
                        Log.e("rotation bottom : ", dx1 + " "+dy1);
                        finalNode.setLocalRotation(localrotation);
                    }*/

                }


            } else if (pointerCount == 2) {
                if (fingersAreClosing) {
                    touchStatus = TOUCH_STATUS_ZOOMING_CAMERA;
                    Log.i("touched", "Zoom In");
                  /*  Camera camera = scene.getCamera();
                    Vector3 cameraPosition = camera.getWorldPosition();
                    float zoomFactor = (length - previousLength);
                    Log.i("factor", String.valueOf(zoomFactor));

                        if (zoomFactor > 3 && cameraPosition.z > -0.499) {
                            cameraPosition.z -= 0.08f;
                            Log.i("z", String.valueOf(cameraPosition.z));
                        } else if (zoomFactor < 3 && cameraPosition.z < 1.47) {
                            cameraPosition.z += 0.08f;
                            Log.i("z", String.valueOf(cameraPosition.z));
                        }
                        camera.setWorldPosition(cameraPosition);*/
                   /* if (isRotating) {
					touchStatus = TOUCH_STATUS_ROTATING_CAMERA;
					Log.i(TAG, "Rotating camera '" + Math.signum(rotationVector[2]) + "'...");
					Camera camera = scene.getCamera();

					//camera.Rotate((float) (Math.signum(rotationVector[2]) / Math.PI) / 4);
				}
*/
                }


            }
        }
        previousX1 = x1;
        previousY1 = y1;
        previousX2 = x2;
        previousY2 = y2;

        previousRotationSquare = currentSquare;

        System.arraycopy(vector, 0, previousVector, 0, vector.length);

        if (gestureChanged && touchDelay > 1) {
            gestureChanged = false;
            Log.v(TAG, "Fin");
        }


        return true;

    }


    private void createCircle(HitTestResult hitTestResult, TransformableNode finalNode) {
            Log.e("Hit at : ", hitTestResult.getNode().getName());
            if (!hitTestResult.getNode().getName().equals("Injection")) {
                MaterialFactory.makeOpaqueWithColor(mContext, new Color(android.graphics.Color.RED))
                        .thenAccept(
                                new Consumer<Material>() {
                                    @Override
                                    public void accept(Material material) {
                                        redSphereRenderable =
                                                ShapeFactory.makeCylinder(0.03f, 0, new Vector3(0f, 0, 0f), material);
                                        addNodeToScene1(redSphereRenderable, hitTestResult, finalNode);
                                    }
                                });
            }

    }

    private void addNodeToScene1(ModelRenderable modelRenderable, HitTestResult hitTestResult, TransformableNode finalNode) {
        Node modelNode = new Node();
        modelNode.setParent(finalNode);
        modelNode.setWorldPosition(hitTestResult.getPoint());
        modelNode.setRenderable(modelRenderable);
        modelNode.setEnabled(true);

        modelNode.setName("Injection");
        modelNode.setLocalRotation(new Quaternion(0.5f, 0.5f, 0.5f, 0.5f));
        modelNode.setParent(finalNode);


    }


}

class TouchScreen {

    // these matrices will be used to move and zoom image
    private android.graphics.Matrix matrix = new android.graphics.Matrix();
    private android.graphics.Matrix savedMatrix = new android.graphics.Matrix();
    // we can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    // remember some things for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float d = 0f;
    private float newRot = 0f;
    private float[] lastEvent = null;

    public boolean onTouch(View v, MotionEvent event) {
        // handle touch events here
        ImageView view = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = getRotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    if (lastEvent != null && event.getPointerCount() == 3) {
                        newRot = getRotation(event);
                        float r = newRot - d;
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float tx = values[2];
                        float ty = values[5];
                        float sx = values[0];
                        float xc = (view.getWidth() / 2) * sx;
                        float yc = (view.getHeight() / 2) * sx;
                        matrix.postRotate(r, tx + xc, ty + yc);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix);
        return true;
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Calculate the degree to be rotated by.
     *
     * @param event
     * @return Degrees
     */
    public static float getRotation(MotionEvent event) {
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(Math.abs(dy), Math.abs(dx));
        double degrees = Math.toDegrees(radians);
        return (float) degrees;
    }

    public static float getRotation360(MotionEvent event) {
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(Math.abs(dy), Math.abs(dx));
        double degrees = Math.toDegrees(radians);
        int square = 1;
        if (dx > 0 && dy == 0) {
            square = 1;
        } else if (dx > 0 && dy < 0) {
            square = 1;
        } else if (dx == 0 && dy < 0) {
            square = 2;
            degrees = 180 - degrees;
        } else if (dx < 0 && dy < 0) {
            square = 2;
            degrees = 180 - degrees;
        } else if (dx < 0 && dy == 0) {
            square = 3;
            degrees = 180 + degrees;
        } else if (dx < 0 && dy > 0) {
            square = 3;
            degrees = 180 + degrees;
        } else if (dx == 0 && dy > 0) {
            square = 4;
            degrees = 360 - degrees;
        } else if (dx > 0 && dy > 0) {
            square = 4;
            degrees = 360 - degrees;
        }
        return (float) degrees;
    }

    public static int getSquare(MotionEvent event) {
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        int square = 1;
        if (dx > 0 && dy == 0) {
            square = 1;
        } else if (dx > 0 && dy < 0) {
            square = 1;
        } else if (dx == 0 && dy < 0) {
            square = 2;
        } else if (dx < 0 && dy < 0) {
            square = 2;
        } else if (dx < 0 && dy == 0) {
            square = 3;
        } else if (dx < 0 && dy > 0) {
            square = 3;
        } else if (dx == 0 && dy > 0) {
            square = 4;
        } else if (dx > 0 && dy > 0) {
            square = 4;
        }
        return square;
    }
}
