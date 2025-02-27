package com.team254.lib.trajectory.timing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;

import com.team254.lib.geometry.Pose2dWithCurvature;
import com.team254.lib.geometry.Rotation2dState;
import com.team254.lib.trajectory.Path;
import com.team254.lib.trajectory.PathDistanceSampler;
import com.team254.lib.trajectory.Trajectory;
import com.team254.lib.trajectory.timing.TimingConstraint.MinMaxAcceleration;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class TimingUtilTest {

    public static final double kTestEpsilon = 1e-12;
    
    public static final List<Pose2dWithCurvature> kWaypoints = Arrays.asList(
            new Pose2dWithCurvature(new Pose2d(new Translation2d(0.0, 0.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(24.0, 0.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(36.0, 12.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(60.0, 12.0), new Rotation2d()), 0));


    public static final List<Rotation2dState> kHeadings = List.of(
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(0));

    public Trajectory buildAndCheckTrajectory(
            final PathDistanceSampler dist_view,
            double step_size,
            List<TimingConstraint> constraints,
            double start_vel,
            double end_vel,
            double max_vel,
            double max_acc) {
        Trajectory timed_traj = TimingUtil
                .timeParameterizeTrajectory(dist_view, step_size, constraints, start_vel, end_vel, max_vel,
                        max_acc);
        checkTrajectory(timed_traj, constraints, start_vel, end_vel, max_vel, max_acc);
        return timed_traj;
    }

    public void checkTrajectory(
            final Trajectory traj,
            List<TimingConstraint> constraints,
            double start_vel,
            double end_vel,
            double max_vel,
            double max_acc) {
        assertFalse(traj.isEmpty());
        assertEquals(traj.getPoint(0).state().velocity(), start_vel, kTestEpsilon);
        assertEquals(traj.getPoint(traj.length() - 1).state().velocity(), end_vel, kTestEpsilon);

        // Go state by state, verifying all constraints are satisfied and integration is
        // correct.
        for (int i = 0; i < traj.length(); ++i) {
            final TimedPose state = traj.getPoint(i).state();
            for (final TimingConstraint constraint : constraints) {
                assertTrue(state.velocity() - kTestEpsilon <= constraint.getMaxVelocity(state.state()));
                final MinMaxAcceleration accel_limits = constraint.getMinMaxAcceleration(state.state(),
                        state.velocity());
                assertTrue(state.acceleration() - kTestEpsilon <= accel_limits.max_acceleration());
                assertTrue(state.acceleration() + kTestEpsilon >= accel_limits.min_acceleration());
            }
            if (i > 0) {
                final TimedPose prev_state = traj.getPoint(i - 1).state();
                assertEquals(state.velocity(),
                        prev_state.velocity() + (state.t() - prev_state.t()) * prev_state.acceleration(), kTestEpsilon);
            }
        }
    }

    @Test
    public void testNoConstraints() {
        Path traj = new Path(kWaypoints, kHeadings);
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        // Triangle profile.
        Trajectory timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                new ArrayList<TimingConstraint>(), 0.0, 0.0, 20.0, 5.0);
        System.out.println(timed_traj);

        // Trapezoidal profile.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 0.0, 0.0,
                10.0, 5.0);

        // Trapezoidal profile with start and end velocities.
        timed_traj = buildAndCheckTrajectory(dist_view, 1.0, new ArrayList<TimingConstraint>(), 5.0, 2.0,
                10.0, 5.0);
    }

    @Test
    public void testConditionalVelocityConstraint() {
        Path traj = new Path(kWaypoints, kHeadings);
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        class ConditionalTimingConstraint<S extends Pose2dWithCurvature> implements TimingConstraint {
            @Override
            public double getMaxVelocity(Pose2dWithCurvature state) {
                if (state.getPose().getTranslation().getX() >= 24.0) {
                    return 5.0;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            }

            @Override
            public TimingConstraint.MinMaxAcceleration getMinMaxAcceleration(Pose2dWithCurvature state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }

        // Trapezoidal profile.
        Trajectory timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint<>()), 0.0, 0.0, 10.0, 5.0);
        System.out.println(timed_traj);

    }

    @Test
    public void testConditionalAccelerationConstraint() {
        Path traj = new Path(kWaypoints, kHeadings);
        PathDistanceSampler dist_view = new PathDistanceSampler(traj);

        class ConditionalTimingConstraint implements TimingConstraint {
            @Override
            public double getMaxVelocity(Pose2dWithCurvature state) {
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public TimingConstraint.MinMaxAcceleration getMinMaxAcceleration(Pose2dWithCurvature state,
                    double velocity) {
                return new TimingConstraint.MinMaxAcceleration(-10.0, 10.0 / velocity);
            }
        }

        // Trapezoidal profile.
        Trajectory timed_traj = buildAndCheckTrajectory(dist_view,
                1.0,
                Arrays.asList(new ConditionalTimingConstraint()), 0.0, 0.0, 10.0, 5.0);
        System.out.println(timed_traj);
    }

}
