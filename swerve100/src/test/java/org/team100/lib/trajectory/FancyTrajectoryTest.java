package org.team100.lib.trajectory;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.swerve.ChassisSpeeds;

import com.team254.frc2022.planners.DriveMotionPlanner;
import com.team254.lib.geometry.Pose2dWithCurvature;
import com.team254.lib.geometry.Rotation2dState;
import com.team254.lib.trajectory.Trajectory;
import com.team254.lib.trajectory.TrajectoryTimeIterator;
import com.team254.lib.trajectory.TrajectoryTimeSampler;
import com.team254.lib.trajectory.timing.CentripetalAccelerationConstraint;
import com.team254.lib.trajectory.timing.TimedPose;
import com.team254.lib.trajectory.timing.TimedRotation;
import com.team254.lib.trajectory.timing.TimingConstraint;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Timer;

/**
 * Experiments with the 254 trajectory classes.
 * 
 * The 254 representation is different from the WPI one:
 * 
 * what they call a "trajectory" is parameterized by state.
 * 
 * usually the parameter is a "timed state", which simply combines a state
 * with time, like the WPI trajectory state. again the timed state is
 * parameterized, usually instantiated with Pose2dWithCurvature and Rotation2d.
 * 
 * sometimes the parameter does not include time, e.g. it's just pose and
 * rotation, and in those cases the resulting trajectory is really a "path", and
 * it is converted later, using TimingUtil.timeParameterizeTrajectory.
 * 
 * a trajectory represents the path with poses, like WPI, and it also includes
 * heading.
 * 
 * Some of the trajectory-related stuff uses *inches* as the length measure but
 * i think it's actually unitless, i.e. whatever you give in the waypoints is
 * the same as the velocity limits etc.
 * 
 * There are many issues here. the trajectory generator doesn't listen to all
 * its inputs, and it has some static constraints. So we should make our own.
 * 
 * Trajectories are followed by DriveMotionPlanner, in code that i didn't move
 * over; maybe i should put that other code back.
 * 
 */
public class FancyTrajectoryTest {
    private static final double kDelta = 0.001;

    /**
     * This is derived from one of the auton trajectories in
     * TrajectoryGenerator.getFarRightStartToFarRightBallHalf
     * it moves to the right in a straight line.
     */
    @Test
    void testLikeAuton() {
        final double kMaxVel = 1.0;
        final double kMaxAccel = 1.0;
        // this doesn't actually do anything.
        final double kMaxVoltage = 9.0;

        // first right and then ahead
        List<Pose2d> waypoints = List.of(
                new Pose2d(0, 0, Rotation2d.fromDegrees(270)),
                new Pose2d(10, -10, Rotation2d.fromDegrees(0)));
        // while turning 180
        List<Rotation2dState> headings = List.of(
                GeometryUtil.fromDegrees(90),
                GeometryUtil.fromDegrees(180));
        // these don't actually do anything.
        List<TimingConstraint> constraints = List.of(
                new CentripetalAccelerationConstraint(60));

        // note there are static constraints in here.
        DriveMotionPlanner mMotionPlanner = new DriveMotionPlanner();
        double start_vel = 0;
        double end_vel = 0;
        // there's a bug in here; it doesn't use the constraints, nor the voltage.
        Trajectory trajectory = mMotionPlanner.generateTrajectory(
                waypoints,
                headings,
                constraints,
                start_vel,
                end_vel,
                kMaxVel,
                kMaxAccel,
                kMaxVoltage);
        System.out.println(trajectory);
        System.out.println("TRAJECTORY LENGTH: " + trajectory.length());
        // assertEquals(10, trajectory.length());

        TrajectoryTimeSampler view = new TrajectoryTimeSampler(trajectory);

        TrajectoryTimeIterator iter = new TrajectoryTimeIterator(view);

        mMotionPlanner.setTrajectory(iter);

        // in the drive loop, this happens:
        Pose2d actualPose = GeometryUtil.kPose2dIdentity;
        double fpgatime = 0;
        final double now = Timer.getFPGATimestamp();
        // this stuff doesn't work, and i don't want to break the build so i'm
        // commenting it out.

        Translation2d translation2d = new Translation2d(1, 1);
        Rotation2d rotation2d = Rotation2d.fromDegrees(90);

        actualPose = new Pose2d(translation2d, rotation2d);

        System.out.println("POSE::::::::::::::::::::::::::::" + actualPose);

        ChassisSpeeds output = mMotionPlanner.update(now, actualPose);

        Translation2d translational_error = mMotionPlanner.getTranslationalError();
        Rotation2dState heading_error = mMotionPlanner.getHeadingError();
        TimedPose path_setpoint = mMotionPlanner.getPathSetpoint();
        TimedRotation heading_setpoint = mMotionPlanner.getHeadingSetpoint();

        // the DriveMotionPlanner has two ways to follow the trajectory: it could just
        // follow it,
        // or it could look into the future trajectory states, and chase those, using
        // "pure pursuit."
        // the "future" is pretty far away, 0.25 seconds. I guess the difference is that
        // the "pure pursuit"
        // model corects errors in the direction of the future state, whereas the pure
        // tracking model
        // aims for the *current* state, which seems worse.

    }

}
