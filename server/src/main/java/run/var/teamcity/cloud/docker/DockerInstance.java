package run.var.teamcity.cloud.docker;

import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.serverSide.AgentDescription;
import run.var.teamcity.cloud.docker.util.DockerCloudUtils;
import run.var.teamcity.cloud.docker.util.LockHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * A Docker {@link CloudInstance}.
 */
public class DockerInstance implements CloudInstance, DockerCloudErrorHandler {

    private final UUID uuid = UUID.randomUUID();
    private final DockerImage img;

    private Instant startedTime;

    // This lock ensure a thread-safe usage of all the variables below.
    private final LockHandler lock = LockHandler.newReentrantLock();

    private String containerName = null;
    private String containerId;
    private ContainerInfo containerInfo;
    private InstanceStatus status = InstanceStatus.UNKNOWN;
    private CloudErrorInfo errorInfo;

    /**
     * Creates a new Docker cloud instance.
     *
     * @param img the source image
     *
     * @throws NullPointerException if {@code img} is {@code null}
     */
    DockerInstance(@Nonnull DockerImage img) {
        this.img = DockerCloudUtils.requireNonNull(img, "Docker image cannot be null.");

        // The instance is expected to be started immediately (we must do this to ensure that getStartedTime() always
        // return some meaningful value).
        updateStartedTime();
    }

    /**
     * The instance UUID.
     *
     * @return the instance UUID.
     */
    @Nonnull
    UUID getUuid() {
        return uuid;
    }

    @Nonnull
    @Override
    public String getInstanceId() {
        return uuid.toString();
    }

    /**
     * Gets the Docker container ID associated with this cloud instance. It could be {@code null} if the container is
     * not known yet or is not available anymore.
     *
     * @return the container ID or {@code null}
     */
    @Nullable
    String getContainerId() {
        return containerId;
    }

    /**
     * Sets the Docker container ID.
     *
     * @param containerId the container ID
     *                    S
     *
     * @throws NullPointerException if {@code containerId} is {@code null}
     */
    void setContainerId(@Nonnull String containerId) {
        DockerCloudUtils.requireNonNull("Container ID cannot be null.", containerId);

        lock.run(() -> this.containerId = containerId);
    }

    @Nonnull
    @Override
    public String getName() {
        return lock.call(() -> containerName == null ? "<Unknown>" : containerName);
    }

    @Nullable
    String getContainerName() {
        return lock.call(() -> containerName);
    }

    /**
     * Sets the instance name.
     *
     * @param containerName the instance name
     *
     * @throws NullPointerException if {@code name} is {@code null}
     */
    void setContainerName(@Nonnull String containerName) {
        DockerCloudUtils.requireNonNull(containerName, "Container name cannot be null.");

        lock.run(() -> this.containerName = containerName);
    }

    @Nonnull
    @Override
    public String getImageId() {
        return img.getId();
    }

    @Nonnull
    @Override
    public DockerImage getImage() {
        return img;
    }

    @Nonnull
    @Override
    public Date getStartedTime() {
        return Date.from(startedTime);
    }

    @Nullable
    @Override
    public String getNetworkIdentity() {
        // Not too sure what we should do here. Obviously, the TC server knows the agent IP address, and it would not
        // makes much sense to retrieve it ourselves from the container configuration. It would be also difficult to
        // return an usable hostname.
        return null;
    }

    @Nonnull
    @Override
    public InstanceStatus getStatus() {
        return status;
    }

    /**
     * Set this instance status.
     *
     * @param status the instance status
     *
     * @throws NullPointerException if {@code status} is {@code null}
     */
    void setStatus(@Nonnull InstanceStatus status) {
        DockerCloudUtils.requireNonNull(status, "Instance status cannot be null.");

        lock.run(() -> this.status = status);

    }

    final void updateStartedTime() {
        lock.run(() -> startedTime = Instant.now());
    }

    @Nullable
    @Override
    public CloudErrorInfo getErrorInfo() {
        return errorInfo;
    }

    /**
     * Gets the additional container meta-data retrieved from the Docker daemon.
     *
     * @return the additional container meta-data or {@code null} if not available
     */
    @Nonnull
    public Optional<ContainerInfo> getContainerInfo() {
        return Optional.ofNullable(lock.call(() -> containerInfo));
    }

    /**
     * Sets the additional container meta-data.
     *
     * @param containerInfo the container meta-data or {@code null} if not available
     */
    void setContainerInfo(@Nullable ContainerInfo containerInfo) {
        lock.run(() -> this.containerInfo = containerInfo);
    }

    @Override
    public void notifyFailure(@Nonnull String msg, @Nullable Throwable throwable) {
        DockerCloudUtils.requireNonNull(msg, "Message cannot be null.");

        lock.run(() -> {
            if (throwable != null) {
                this.errorInfo = new CloudErrorInfo(msg, msg, throwable);
            } else {
                this.errorInfo = new CloudErrorInfo(msg, msg);
            }

            setStatus(InstanceStatus.ERROR);
        });

    }

    @Override
    public boolean containsAgent(@Nonnull AgentDescription agent) {
        return uuid.equals(DockerCloudUtils.getInstanceId(agent));
    }

    @Override
    public String toString() {
        return getName();
    }
}
