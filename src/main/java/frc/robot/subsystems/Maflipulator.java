package frc.robot.subsystems;

import com.chopshop166.chopshoplib.outputs.SendableSpeedController;

import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.InstantCommand;
import edu.wpi.first.wpilibj.command.PIDCommand;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.interfaces.Potentiometer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;
import frc.robot.RobotMap;

public class Maflipulator extends Subsystem {

    public enum MaflipulatorSide {
        kFront, kBack;
    }

    private final static double FRONT_LOWER_ANGLE = 0.94;
    private final static double FRONT_SCORING_ANGLE = 90;
    private final static double FRONT_FLIP_POSITION = FRONT_SCORING_ANGLE;
    private final static double FRONT_UPPER_ANGLE = 0.69;
    private final static double BACK_LOWER_ANGLE = .099;
    private final static double BACK_SCORING_ANGLE = 270;
    private final static double BACK_FLIP_POSITION = BACK_SCORING_ANGLE;
    private final static double BACK_UPPER_ANGLE = .42;

    private final static double FLIP_MOTOR_SPEED = 1;

    private MaflipulatorSide currentPosition;

    private SendableSpeedController flipMotor;
    private Potentiometer anglePot;

    double angleCorrection;
    PIDController anglePID;

    public Maflipulator(final RobotMap.MaflipulatorMap map) { // NOPMD
        super();
        flipMotor = map.getFlipMotor();
        anglePot = map.getMaflipulatorPot();

        anglePID = new PIDController(.01, .0009, 0.0, 0.0, anglePot, (double value) -> {
            angleCorrection = value;
        });

        if (anglePot.get() > 0.5)
            currentPosition = MaflipulatorSide.kFront;
        else
            currentPosition = MaflipulatorSide.kBack;

    }

    public void addChildren() {
        addChild(flipMotor);
        addChild(anglePot);
    }

    @Override
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        setDefaultCommand(manualRotate());
    }

    protected double restrict(double flipSpeed) {
        if (currentPosition == MaflipulatorSide.kFront) {
            if (flipSpeed > 0 && anglePot.get() <= FRONT_UPPER_ANGLE) {
                flipSpeed = 0;
            }
            if (flipSpeed < 0 && anglePot.get() >= FRONT_LOWER_ANGLE) {
                flipSpeed = 0;
            }
        } else {
            if (flipSpeed < 0 && anglePot.get() >= BACK_UPPER_ANGLE) {
                flipSpeed = 0;
            }
            if (flipSpeed > 0 && anglePot.get() <= BACK_LOWER_ANGLE) {
                flipSpeed = 0;
            }
        }
        SmartDashboard.putNumber("Pot Angle", anglePot.get());
        return flipSpeed;

    }

    public Command manualRotate() {
        // The command is named "Manual Rotate" and requires this subsystem.
        return new Command("Manual Rotate", this) {

            @Override
            protected void execute() {
                double flipSpeed = Robot.xBoxCoPilot.getY(Hand.kLeft) * FLIP_MOTOR_SPEED;
                flipSpeed = restrict(flipSpeed);
                flipMotor.set(flipSpeed);
                SmartDashboard.putString("Side", currentPosition.toString());
                SmartDashboard.putNumber("Flip Speed", flipSpeed);
            }

            @Override
            protected boolean isFinished() {
                return false;
            }

        };
    }

    public Command Flip() {
        return new InstantCommand("Flip", this, () -> {

            Command moveCommand;
            if (currentPosition == MaflipulatorSide.kFront) {
                moveCommand = moveToPosition(FRONT_FLIP_POSITION);
                currentPosition = MaflipulatorSide.kBack;
            } else {
                moveCommand = moveToPosition(BACK_FLIP_POSITION);
                currentPosition = MaflipulatorSide.kFront;
            }

            moveCommand.start();
        });
    }

    public Command CrappyFlip() {
        return new Command("Crappy Flip", this) {
            @Override
            protected void initialize() {
                if (currentPosition == MaflipulatorSide.kFront) {
                    flipMotor.set(0.5);

                } else {
                    flipMotor.set(-0.5);

                }
            }

            @Override
            protected boolean isFinished() {

                if (anglePot.get() <= 0.25 && currentPosition == MaflipulatorSide.kFront) {
                    return true;
                }

                if (anglePot.get() >= 0.7 && currentPosition == MaflipulatorSide.kBack) {
                    return true;
                }

                return false;
            }

            @Override
            protected void end() {
                flipMotor.set(0);
                if (currentPosition == MaflipulatorSide.kFront) {
                    currentPosition = MaflipulatorSide.kBack;
                }
                if (currentPosition == MaflipulatorSide.kBack) {
                    currentPosition = MaflipulatorSide.kFront;
                }

            }
        };
    }

    public Command PIDScoringPosition() {
        return new InstantCommand("PID Scoring Position", this, () -> {

            Command moveCommand;
            if (currentPosition == MaflipulatorSide.kFront)
                moveCommand = moveToPosition(FRONT_SCORING_ANGLE);
            else
                moveCommand = moveToPosition(BACK_SCORING_ANGLE);

            moveCommand.start();
        });

    }

    public Command PIDPickupPosition() {
        return new InstantCommand("PID Pickup Position", this, () -> {

            Command moveCommand;
            if (currentPosition == MaflipulatorSide.kFront)
                moveCommand = moveToPosition(FRONT_LOWER_ANGLE);
            else
                moveCommand = moveToPosition(BACK_LOWER_ANGLE);

            moveCommand.start();
        });
    }

    public Command moveToPosition(double targetPosition) {
        return new PIDCommand("Move to Position", .01, .0009, 0.0, this) {

            @Override
            protected void initialize() {
                anglePID.reset();
                anglePID.setSetpoint(targetPosition);
                anglePID.enable();
            }

            @Override
            protected boolean isFinished() {
                return anglePID.onTarget() && Math.abs(angleCorrection) < 0.1;
            }

            @Override
            protected void end() {
                flipMotor.set(0);
            }

            @Override
            protected double returnPIDInput() {
                return anglePot.pidGet();
            }

            @Override
            protected void usePIDOutput(double output) {
                double flipSpeed = angleCorrection;
                flipSpeed = restrict(flipSpeed);
                flipMotor.set(flipSpeed);
            }
        };
    }
}
