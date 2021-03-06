package frc.robot.subsystems;

import com.chopshop166.chopshoplib.commands.CommandChain;
import com.chopshop166.chopshoplib.outputs.SendableSpeedController;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.InstantCommand;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.command.TimedCommand;
import edu.wpi.first.wpilibj.command.WaitCommand;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.RobotMap;
import frc.robot.triggers.LimitSwitchTrigger;

public class Manipulator extends Subsystem {

    private SendableSpeedController rollersMotor;
    private DoubleSolenoid beaksPiston;
    private DoubleSolenoid armsPiston;
    private DigitalInput gamepieceLimitSwitch;
    private DigitalInput foldedBackLimitSwitch;
    private DigitalInput intakePositionLimitSwitch;
    public LimitSwitchTrigger switchTrigger;

    public Manipulator(final RobotMap.ManipulatorMap map) { // NOPMD
        super();
        // Take values that the subsystem needs from the map, and store them in the
        // class
        rollersMotor = map.getrollersMotor();
        beaksPiston = map.getbeaksPiston();
        armsPiston = map.getArmsPiston();
        gamepieceLimitSwitch = map.getGamepieceLimitSwitch();
        foldedBackLimitSwitch = map.getfoldedBackLimitSwitch();
        intakePositionLimitSwitch = map.getintakePositionLimitSwitch();
        switchTrigger = new LimitSwitchTrigger(gamepieceLimitSwitch);
        addChildren();
        SmartDashboard.putData(openBeak());
        SmartDashboard.putData(closeBeak());
    }

    public void addChildren() {
        addChild(rollersMotor);
        addChild(beaksPiston);
        addChild(armsPiston);
        addChild(gamepieceLimitSwitch);
        addChild(foldedBackLimitSwitch);
        addChild(intakePositionLimitSwitch);
    }

    double rollerspeed = .7;

    @Override
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        // setDefaultCommand(new MySpecialCommand());

    }

    // #region Command Chains
    public Command pickUpCargo() {
        CommandChain retValue = new CommandChain("Pick up Cargo");
        retValue.then(openBeak()).then(rollerIntake()).then(retractArms()).then(gamepieceCheck()).then(rollerStop());
        return retValue;
    }

    /*
     * public Command ledHatchPanel() { if (gamepieceLimitSwitch.get()) { return new
     * InstantCommand("turn on the green color", this, () -> {
     * Robot.leds.turnOnGreen(true); });
     * 
     * } }
     */

    public Command releaseCargo() {
        CommandChain retValue = new CommandChain("Release Cargo");
        retValue.then(rollerEject()).then(gamepieceCheck()).then(new WaitCommand(.5)).then(rollerStop());
        return retValue;
    }

    public Command pickUpHatch() {
        CommandChain retValue = new CommandChain("Pick Up Hatch");
        retValue.then(closeBeak()).then(retractArms()).then(gamepieceCheck()).then(openBeak());
        return retValue;
    }

    // #endregion

    // #region Commands
    public Command openBeak() {
        return new InstantCommand("Open Beak", this, () -> {
            beaksPiston.set(Value.kForward);
        });
    }

    public Command closeBeak() {
        return new InstantCommand("Close Beak", this, () -> {
            beaksPiston.set(Value.kReverse);
        });
    }

    public Command extendArms() {
        return new InstantCommand("Extend Arms", this, () -> {
            armsPiston.set(Value.kForward);
        });
    }

    public Command retractArms() {
        return new InstantCommand("Retract Arms", this, () -> {
            armsPiston.set(Value.kReverse);
        });
    }

    public Command rollerIntake() {
        return new InstantCommand("Intake Rollers", this, () -> {
            rollersMotor.set(rollerspeed);
        });
    }

    public Command rollerEject() {
        return new InstantCommand("Eject Rollers", this, () -> {
            rollersMotor.set(-rollerspeed);
        });
    }

    public Command rollerStop() {
        return new InstantCommand("Stop Rollers", this, () -> {
            rollersMotor.set(0);
        });
    }

    public Command gamepieceCheck() {
        // This command will check if a gamepiece is held
        return new Command("Check for Gamepiece", this) {

            @Override
            protected boolean isFinished() {
                return gamepieceLimitSwitch.get();
            }
        };
    }
    // #endregion

    public Command Intake() {
        return new Command("Intake Ball", this) {

            @Override
            protected void initialize() {
                armsPiston.set(Value.kForward);
                rollersMotor.set(rollerspeed);
            }

            @Override
            protected boolean isFinished() {
                return false;
            }

            @Override
            protected void end() {
                armsPiston.set(Value.kReverse);
                rollersMotor.set(0);
            }
        };
    }

    public TimedCommand Eject() {
        return new TimedCommand("Eject Ball", 1, this) {

            @Override
            protected void initialize() {
                rollersMotor.set(-rollerspeed);
            }

            @Override
            protected void end() {
                rollersMotor.set(0);
            }
        };
    }
}
