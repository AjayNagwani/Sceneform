package com.test.sceneformapp;


import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.collision.RayHit;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.BaseTransformationController;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.PinchGesture;
import com.google.ar.sceneform.ux.PinchGestureRecognizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {

    Scene scene;
    SceneView sceneView;
    Camera camera;
    private float scale = 1f;
    BaseTransformationController controller;
    PinchGesture pinchGesture;
    PinchGestureRecognizer recognizer;
    private String TAG = "MainActivity.this";
    private TransformationSystem transformationSystem;
    private TransformableNode finalNode;
    private TouchController touchHandler;
    private ModelRenderable redSphereRenderable;
    private TransformableNode boundsNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sceneView = findViewById(R.id.sceneView);
        scene = sceneView.getScene();
        camera = scene.getCamera();
        camera.setWorldPosition(new Vector3(0, 0.6f, 1.5f));
        transformationSystem = new TransformationSystem(getResources().getDisplayMetrics(), new FootprintSelectionVisualizer());
        finalNode = new TransformableNode(transformationSystem);
        //  finalNode.setParent(scene);
        finalNode.select();
        finalNode.setName("human");
        finalNode.getScaleController().setEnabled(true);
        finalNode.getRotationController().setEnabled(true);
        touchHandler = new TouchController(scene);
        sceneView.setOnTouchListener(new GestureDetector.OnDoubleTapListener(this) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent motionEvent) {
                return false;
            }
        });
        scene.addOnPeekTouchListener(new Scene.OnPeekTouchListener() {
            @Override
            public void onPeekTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
                try {
                    touchHandler.onTouchEvent(motionEvent, boundsNode, hitTestResult, getApplicationContext());
                    transformationSystem.onTouch(hitTestResult, motionEvent);
                    Log.e("Scene hit : ", hitTestResult.getNode().getName());

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        renderObject(Uri.parse("human.sfb"));


    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void renderObject(Uri parse) {

        Log.e("Uri", parse.toString());
        ModelRenderable.builder()
                .setSource(this, parse)
                .build()
                .thenAccept(new Consumer<ModelRenderable>() {
                    @Override
                    public void accept(ModelRenderable modelRenderable) {
                        MainActivity.this.addNodeToScene(modelRenderable);
                        makeBox(modelRenderable);
                    }
                })
                .exceptionally(
                        throwable -> {
                            Log.e(String.valueOf(MainActivity.this), "Unable to load Renderable.", throwable);
                            return null;
                        });


    }

    private void makeBox(ModelRenderable modelRenderable) {
        boundsNode = new TransformableNode(transformationSystem);
        MaterialFactory.makeTransparentWithColor(this, new Color(0.8f, 0.8f, 0.8f, 0.5f))
                .thenAccept(
                        material -> {
                            Box box = (Box) modelRenderable.getCollisionShape();
                            Renderable renderable = modelRenderable;
                            renderable.setMaterial(material);
                            renderable.setCollisionShape(modelRenderable.getCollisionShape());
                            boundsNode.setRenderable(renderable);
                            boundsNode.setParent(scene);
                        });
        /*boundsNode.setCollisionShape(modelRenderable.getCollisionShape());
        boundsNode.setParent(finalNode);*/
    }

    private void addNodeToScene(ModelRenderable modelRenderable) {

        finalNode.setRenderable(modelRenderable);
        finalNode.setLocalPosition(new Vector3(0, 0f, 0f));
        finalNode.setEnabled(true);
        finalNode.select();
        finalNode.getScaleController().setEnabled(true);
        finalNode.getRotationController().setEnabled(false);
        finalNode.getTranslationController().getActiveGesture();


    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            sceneView.resume();

        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sceneView.pause();
    }
}
