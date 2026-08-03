package fr.corehost.lobby.parkour;

import org.bukkit.Location;

public class ActiveParkourSession {
    private final ParkourCourse course;
    private final long startTime;
    private int currentCheckpointIndex;
    private Location lastCheckpointLocation;

    public ActiveParkourSession(ParkourCourse course, long startTime, Location initialLocation) {
        this.course = course;
        this.startTime = startTime;
        this.currentCheckpointIndex = 0;
        this.lastCheckpointLocation = initialLocation;
    }

    public ParkourCourse getCourse() {
        return course;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getCurrentCheckpointIndex() {
        return currentCheckpointIndex;
    }

    public void setCurrentCheckpointIndex(int currentCheckpointIndex) {
        this.currentCheckpointIndex = currentCheckpointIndex;
    }

    public Location getLastCheckpointLocation() {
        return lastCheckpointLocation;
    }

    public void setLastCheckpointLocation(Location lastCheckpointLocation) {
        this.lastCheckpointLocation = lastCheckpointLocation;
    }
}
