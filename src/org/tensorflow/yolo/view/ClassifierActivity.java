package org.tensorflow.yolo.view;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.Toast;

import org.tensorflow.yolo.R;
import org.tensorflow.yolo.TensorFlowImageRecognizer;
import org.tensorflow.yolo.model.Recognition;
import org.tensorflow.yolo.util.ImageUtils;
import org.tensorflow.yolo.view.components.BorderedText;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import static android.graphics.ImageFormat.JPEG;
import static android.os.SystemClock.sleep;
import static org.tensorflow.yolo.Config.INPUT_SIZE;
import static org.tensorflow.yolo.Config.LOGGING_TAG;

/**
 * Classifier activity class
 * Modified by Zoltan Szabo
 */
public class ClassifierActivity extends TextToSpeechActivity implements OnImageAvailableListener {
    private boolean MAINTAIN_ASPECT = true;
    private float TEXT_SIZE_DIP = 10;

    private TensorFlowImageRecognizer recognizer;
    private Integer sensorOrientation;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private Bitmap croppedBitmap = null;
    private boolean computing = false;
    private Matrix frameToCropTransform;

    private OverlayView overlayView;
    private BorderedText borderedText;
    private long lastProcessingTimeMs;
    private String ok;
    private Image img;
    List<Recognition> resultados;
    VerifyClassification verificacao = new VerifyClassification();
    Bitmap var;

    ImageSave imgSave = new ImageSave();

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        recognizer = TensorFlowImageRecognizer.create(getAssets());

        overlayView = (OverlayView) findViewById(R.id.overlay);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final int screenOrientation = getWindowManager().getDefaultDisplay().getRotation();

        Log.i(LOGGING_TAG, String.format("Sensor orientation: %d, Screen orientation: %d",
                rotation, screenOrientation));

        sensorOrientation = rotation + screenOrientation;

        Log.i(LOGGING_TAG, String.format("Initializing at size %dx%d", previewWidth, previewHeight));

        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight,
                INPUT_SIZE, INPUT_SIZE, sensorOrientation, MAINTAIN_ASPECT);
        frameToCropTransform.invert(new Matrix());

        addCallback((final Canvas canvas) -> renderAdditionalInformation(canvas));
        ImageSave btn = new ImageSave();
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;
        //Preciso pegar esse objeto imagem e salvar
//        reader = ImageReader.newInstance(1080, 720, JPEG, 10);
        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (computing) {
                image.close();
                return;
            }

            computing = true;
            var = fillCroppedBitmap(image);
            var = this.rotateBit(var);
            image.close();
        } catch (final Exception ex) {
            if (image != null) {
                image.close();
            }
            Log.e(LOGGING_TAG, ex.getMessage());
        }

        Image finalImage = image;
        AtomicReference<Bitmap> bitmap = null;
        runInBackground(() -> {
            final long startTime = SystemClock.uptimeMillis();
            final List<Recognition> results = recognizer.recognizeImage(croppedBitmap);
            resultados = results;
            img =  finalImage;
            if (resultados != null){
                ok = verificacao.verifyList(results);
                if (ok == "Achou"){
                    MediaStore.Images.Media.insertImage(getContentResolver(), var, "Lalala" , "lululu");
                    Toast.makeText(this,
                            "Imagem Salva", Toast.LENGTH_LONG).show();
                }
            }
            else{
                ok = verificacao.verifyList(results);
            }
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            overlayView.setResults(results);
            speak(results);
            requestRender();
            computing = false;
        });
    }

    private Bitmap fillCroppedBitmap(final Image image) {
            Bitmap rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
            rgbFrameBitmap.setPixels(ImageUtils.convertYUVToARGB(image, previewWidth, previewHeight),
                    0, previewWidth, 0, 0, previewWidth, previewHeight);
            new Canvas(croppedBitmap).drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        return rgbFrameBitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.close();
        }
    }

    public Bitmap rotateBit(Bitmap bitmap)
    {
        Matrix matrix = new Matrix();

        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        return  rotatedBitmap;
    }

    private void renderAdditionalInformation(final Canvas canvas) {
        final Vector<String> lines = new Vector();
        final Vector<Button> buttons = new Vector();
        Button button = null;
        if (recognizer != null) {
            for (String line : recognizer.getStatString().split("\n")) {
                lines.add(line);
            }
        }
        buttons.add(button);

        lines.add("Frame: " + previewWidth + "x" + previewHeight);
        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
        lines.add("Rotation: " + sensorOrientation);
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        lines.add("Resultados: " + resultados);
        lines.add("Ok: " + ok);

        borderedText.drawLines(canvas, 10, 10, lines);
    }
}
