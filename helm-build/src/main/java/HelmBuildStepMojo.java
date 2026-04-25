import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;

@Mojo(
        name = "helm.build",
        defaultPhase = LifecyclePhase.PACKAGE
)
public class HelmBuildStepMojo extends AbstractMojo {
    private final Log log = getLog();
    @Parameter(property = "helm.build.helmDir", defaultValue = "/deploy/chart")
    private String helmChartDir;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Start to build helm deploy");
        Map.Entry<Boolean, String> checkResult = checkDeployExist();
        if (checkResult.getKey()) {
            log.info("Helm deploy chart not exist at %s".formatted(checkResult.getValue()));
            return;
        }

        log.info("Start to update dependencies helm chart dependencies");
        updateDependencies();
    }

    private Map.Entry<Boolean, String> checkDeployExist() {
        String fullPath = project.getBasedir().getAbsolutePath() + helmChartDir;
        return new AbstractMap.SimpleEntry<>(Files.exists(Path.of(fullPath)), "");
    }

    private void updateDependencies() throws MojoExecutionException {
        String fullPath = project.getBasedir().getAbsolutePath() + helmChartDir;

        try {
            ProcessBuilder pb = new ProcessBuilder("helm", "dependency", "update", fullPath);
            pb.redirectErrorStream(true);

            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                getLog().info(line);
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException("Command failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute command", e);
        }
    }
}
