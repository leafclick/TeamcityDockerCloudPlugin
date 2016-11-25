package run.var.teamcity.cloud.docker.client;

import org.testng.SkipException;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TcpDefaultDockerClientTest extends DefaultDockerClientTest {
    @Override
    protected DefaultDockerClient createClientInternal(int threadPoolSize) throws URISyntaxException {

        String dockerTcpAddress = System.getProperty("docker.test.tcp.address");
        if (dockerTcpAddress == null) {
            throw new SkipException("Java system variable docker.test.tcp.address not set. Skipping TCP based tests.");
        }
        return DefaultDockerClient.open(new URI("tcp://" + dockerTcpAddress), false, threadPoolSize);
    }

    public  void openValidInput() {
        // Missing port.
        DefaultDockerClient.open(URI.create("tcp://127.0.0.1"), false, 1).close();
        DefaultDockerClient.open(URI.create("tcp://127.0.0.1"), true, 1).close();
        DefaultDockerClient.open(URI.create("tcp://127.0.0.1:2375"), false, 1).close();
    }

    public void networkFailure() {
        try (DockerClient client = DefaultDockerClient.open(URI.create("tcp://notanrealhost:2375"), false, 1)) {
            assertThatExceptionOfType(DockerClientProcessingException.class).
                    isThrownBy(client::getVersion);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void openInvalidInput() {


        // Invalid slash count after scheme.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                DefaultDockerClient.open(URI.create("tcp:/127.0.0.1:2375"), false, 1));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                DefaultDockerClient.open(URI.create("tcp:///127.0.0.1:2375"), false, 1));
        // With path.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                DefaultDockerClient.open(URI.create("tcp://127.0.0.1:2375/blah"), false, 1));;
        // With query.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                DefaultDockerClient.open(URI.create("tcp://127.0.0.1:2375?param=value"), false, 1));
    }
}
