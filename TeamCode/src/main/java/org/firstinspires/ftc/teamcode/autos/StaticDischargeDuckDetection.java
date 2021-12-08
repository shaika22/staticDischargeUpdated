/* Copyright (c) 2019 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.teamcode.autos;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.List;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.teamcode.botconfigs.StaticDischargeBot1;


@Autonomous(name = "Sarah Duck Detection", group = "Concept")
public class StaticDischargeDuckDetection extends LinearOpMode {
    /* Note: This sample uses the all-objects Tensor Flow model (FreightFrenzy_BCDM.tflite), which contains
     * the following 4 detectable objects
     *  0: Ball,
     *  1: Cube,
     *  2: Duck,
     *  3: Marker (duck location tape marker)
     *
     *  Two additional model assets are available which only contain a subset of the objects:
     *  FreightFrenzy_BC.tflite  0: Ball,  1: Cube
     *  FreightFrenzy_DM.tflite  0: Duck,  1: Marker
     */
    public StaticDischargeBot1 bot;
    public DcMotor carouselWheel = null;
    //    public Servo leftLatchServo=null;
    public Servo rightLatchServo = null;
    public DcMotor arm = null;
    public Servo flickerServo = null;
    private static final String TFOD_MODEL_ASSET = "FreightFrenzy_BCDM.tflite";
    private static final String[] LABELS = {
            "Ball",
            "Cube",
            "Duck",
            "Marker"
    };

    /*
     * IMPORTANT: You need to obtain your own license key to use Vuforia. The string below with which
     * 'parameters.vuforiaLicenseKey' is initialized is for illustration only, and will not function.
     * A Vuforia 'Development' license key, can be obtained free of charge from the Vuforia developer
     * web site at https://developer.vuforia.com/license-manager.
     *
     * Vuforia license keys are always 380 characters long, and look as if they contain mostly
     * random data. As an example, here is a example of a fragment of a valid key:
     *      ... yIgIzTqZ4mWjk9wd3cZO9T1axEqzuhxoGlfOOI2dRzKS4T0hQ8kT ...
     * Once you've obtained a license key, copy the string from the Vuforia web site
     * and paste it in to your code on the next line, between the double quotes.
     */
    private static final String VUFORIA_KEY =
            "Ae+gmGj/////AAABmWz20p9iPUvOnbOi93QfB7sXbfkCt0bYRo0ZsF9MfCnyyqSzGT50iAvJq63Zsze7uk3efapcDwvsUKu7VS7cI0PKl2NJjJc3WzUzZw66E7qNLah2J06uP5XNWi262fa0EcXDFRazWernOoMDrdd2Rh6W1l5Wo9m6TWPDXeToJWbxoEAlURg7wosy4dIU5tGFcQNZ8B9ZODO+FxzYKUz7HOQmZ2FVHF7kGtWJsk+7ikLsh80gtIQFs6M9qY8gvTyhUPZJKzzvTGSvbbotaVzpzWd4Brvl1w00NXnGy/rVVr/cvN+6bBIN2/S/Qrxx4OhFF01r5eTNDshoiQV9xTJQ2Zvcl7eVB1C8lqe1RdtM8I1L";

    /**
     * {@link #vuforia} is the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    private VuforiaLocalizer vuforia;

    /**
     * {@link #tfod} is the variable we will use to store our instance of the TensorFlow Object
     * Detection engine.
     */
    private TFObjectDetector tfod;

    @Override
    public void runOpMode() {
        // The TFObjectDetector uses the camera frames from the VuforiaLocalizer, so we create that
        // first.
        initVuforia();
        initTfod();

        /**
         * Activate TensorFlow Object Detection before we wait for the start command.
         * Do it here so that the Camera Stream window will have the TensorFlow annotations visible.
         **/
        bot = new StaticDischargeBot1(telemetry, hardwareMap);
        carouselWheel = hardwareMap.dcMotor.get("wheel");
        rightLatchServo = hardwareMap.servo.get("rightlatch");
//        leftLatchServo = hardwareMap.servo.get("leftlatch");
        arm = hardwareMap.dcMotor.get("arm");
        rightLatchServo.setPosition(0.5);
        if (tfod != null) {
            tfod.activate();

            // The TensorFlow software will scale the input images from the camera to a lower resolution.
            // This can result in lower detection accuracy at longer distances (> 55cm or 22").
            // If your target is at distance greater than 50 cm (20") you can adjust the magnification value
            // to artificially zoom in to the center of image.  For best results, the "aspectRatio" argument
            // should be set to the value of the images used to create the TensorFlow Object Detection model
            // (typically 16/9).
            tfod.setZoom(2.5, 16.0 / 9.0);
        }

        /** Wait for the game to begin */
        telemetry.addData(">", "Press Play to start op mode");
        telemetry.update();
        waitForStart();

        boolean duckDetected1 = false;
        boolean duckDetected2 = false;
        boolean duckDetected3 = false;
        int count = 0;

        if (opModeIsActive()) {
            while (opModeIsActive()) {
                if (tfod != null) {

                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                    if (updatedRecognitions != null) {
                        telemetry.addData("# Object Detected", updatedRecognitions.size());

                        // step through the list of recognitions and display boundary info.
                        int i = 0;
                        for (Recognition recognition : updatedRecognitions) {
                            telemetry.addData(String.format("label (%d)", i), recognition.getLabel());

                            if (count == 3) {
                                duckDetected1 = true;
                                break;
                            }
                            if (count == 2) {
                                duckDetected2 = true;
                                bot.driveTrain.moveEncoders(11.5, 0, 0, 0.2);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, -27.25, 0, 0.3);
                                sleep(500);

                                bot.driveTrain.moveEncoders(-2.76, 0, 0, 0.3);
                                sleep(500);
                                carouselWheel.setPower(-0.5);
                                sleep(3000);
                                carouselWheel.setPower(0);
                                bot.driveTrain.moveEncoders(32, 0, 0, 0.6);
                                sleep(500);
//        bot.driveTrain.moveEncoders(0, 0, 0.03, 0.8);
//        sleep(200);
                                bot.driveTrain.moveEncoders(0, 24.5, 0, 0.6);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, 0, 0.66, 0.6);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, -6.75, 0, 0.2);
                                sleep(500);
                                arm.setPower(-0.5);
                                sleep(1000);
                                arm.setPower(0.0);

                                sleep(500);
                                arm.setPower(0.3);
                                sleep(500);
                                //bot.driveTrain.moveEncoders(0, 0, 0.02, 0.5);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, 30, 0, 0.6);
                                sleep(500);

                                bot.driveTrain.moveEncoders(13.25, 0, 0, 0.6);
                                sleep(500);
                                break;
                            }
                            if (count == 1) {
                                duckDetected3 = true;
                                bot.driveTrain.moveEncoders(11.5, 0, 0, 0.2);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, -27.25, 0, 0.3);
                                sleep(500);

                                bot.driveTrain.moveEncoders(-2.76, 0, 0, 0.3);
                                sleep(500);
                                carouselWheel.setPower(-0.5);
                                sleep(3000);
                                carouselWheel.setPower(0);
                                bot.driveTrain.moveEncoders(32, 0, 0, 0.6);
                                sleep(500);
//        bot.driveTrain.moveEncoders(0, 0, 0.03, 0.8);
//        sleep(200);
                                bot.driveTrain.moveEncoders(0, 24.5, 0, 0.6);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, 0, 0.66, 0.6);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, -6.75, 0, 0.2);
                                sleep(500);
                                arm.setPower(-0.5);
                                sleep(1000);
                                arm.setPower(0.0);
                                sleep(1000);
                                flickerServo.setPosition(-0.5);
                                sleep(1000);
                                flickerServo.setPosition(0.5);
                                sleep(500);
                                arm.setPower(0.3);
                                sleep(500);
                                //bot.driveTrain.moveEncoders(0, 0, 0.02, 0.5);
                                sleep(500);
                                bot.driveTrain.moveEncoders(0, 30, 0, 0.6);
                                sleep(500);

                                bot.driveTrain.moveEncoders(13.25, 0, 0, 0.6);
                                sleep(500);
                                break;
                            }
                            count++;

                            i++;
                        }
                        telemetry.update();
                    }
                }
            }

        }
    }

    /**
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "Webcam 1");

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the TensorFlow Object Detection engine.
    }

    /**
     * Initialize the TensorFlow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.8f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 320;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
    }
}
