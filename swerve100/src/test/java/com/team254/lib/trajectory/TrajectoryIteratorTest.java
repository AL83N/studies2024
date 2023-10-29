package com.team254.lib.trajectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;

import com.team254.lib.geometry.Pose2dWithCurvature;
import com.team254.lib.geometry.Rotation2dState;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class TrajectoryIteratorTest {
    public static final double kTestEpsilon = 1e-12;

    public static final List<Pose2dWithCurvature> kWaypoints = Arrays.asList(
            new Pose2dWithCurvature(new Pose2d(new Translation2d(0.0, 0.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(24.0, 0.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(36.0, 12.0), new Rotation2d()), 0),
            new Pose2dWithCurvature(new Pose2d(new Translation2d(60.0, 12.0), new Rotation2d()), 0));

    List<Rotation2dState> kHeadings = Arrays.asList(
            GeometryUtil.fromDegrees(0),
            GeometryUtil.fromDegrees(30),
            GeometryUtil.fromDegrees(60),
            GeometryUtil.fromDegrees(90),
            GeometryUtil.fromDegrees(180));

    @Test
    public void test() {
        Trajectory<Pose2dWithCurvature, Rotation2dState> traj = new Trajectory<>(kWaypoints, kHeadings);
        TrajectoryIterator<Pose2dWithCurvature, Rotation2dState> iterator = new TrajectoryIterator<>(
                traj.getIndexView());

        // Initial conditions.
        assertEquals(0.0, iterator.getProgress(), kTestEpsilon);
        assertEquals(3.0, iterator.getRemainingProgress(), kTestEpsilon);
        assertEquals(kWaypoints.get(0), iterator.getState());
        assertEquals(kHeadings.get(0), iterator.getHeading());
        assertFalse(iterator.isDone());

        // Advance forward.
        assertEquals(kWaypoints.get(0).interpolate2(kWaypoints.get(1), 0.5), iterator.preview(0.5).state());
        assertEquals(kHeadings.get(0).get().interpolate(kHeadings.get(1).get(), 0.5),
                iterator.preview(0.5).heading().get());
        TrajectorySamplePoint<Pose2dWithCurvature, Rotation2dState> newPoint = iterator.advance(0.5);
        assertEquals(kWaypoints.get(0).interpolate2(kWaypoints.get(1), 0.5), newPoint.state());
        assertEquals(kHeadings.get(0).get().interpolate(kHeadings.get(1).get(), 0.5), newPoint.heading().get());
        assertEquals(0.5, iterator.getProgress(), kTestEpsilon);
        assertEquals(2.5, iterator.getRemainingProgress(), kTestEpsilon);
        assertFalse(iterator.isDone());

        // Advance backwards.
        assertEquals(kWaypoints.get(0).interpolate2(kWaypoints.get(1), 0.25), iterator.preview(-0.25).state());
        assertEquals(kHeadings.get(0).get().interpolate(kHeadings.get(1).get(), 0.25),
                iterator.preview(-0.25).heading().get());
        newPoint = iterator.advance(-0.25);
        assertEquals(kWaypoints.get(0).interpolate2(kWaypoints.get(1), 0.25), newPoint.state());
        assertEquals(kHeadings.get(0).get().interpolate(kHeadings.get(1).get(), 0.25), newPoint.heading().get());
        assertEquals(0.25, iterator.getProgress(), kTestEpsilon);
        assertEquals(2.75, iterator.getRemainingProgress(), kTestEpsilon);
        assertFalse(iterator.isDone());

        // Advance past end.
        assertEquals(kWaypoints.get(3), iterator.preview(5.0).state());
        assertEquals(kHeadings.get(3), iterator.preview(5.0).heading());
        newPoint = iterator.advance(5.0);
        assertEquals(kWaypoints.get(3), newPoint.state());
        assertEquals(kHeadings.get(3), newPoint.heading());
        assertEquals(3.0, iterator.getProgress(), kTestEpsilon);
        assertEquals(0.0, iterator.getRemainingProgress(), kTestEpsilon);
        assertTrue(iterator.isDone());

        // Advance past beginning.
        assertEquals(kWaypoints.get(0), iterator.preview(-5.0).state());
        assertEquals(kHeadings.get(0), iterator.preview(-5.0).heading());
        newPoint = iterator.advance(-5.0);
        assertEquals(kWaypoints.get(0), newPoint.state());
        assertEquals(kHeadings.get(0), newPoint.heading());
        assertEquals(0.0, iterator.getProgress(), kTestEpsilon);
        assertEquals(3.0, iterator.getRemainingProgress(), kTestEpsilon);
        assertFalse(iterator.isDone());
    }

}
