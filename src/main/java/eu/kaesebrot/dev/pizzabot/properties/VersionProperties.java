package eu.kaesebrot.dev.pizzabot.properties;

import org.springframework.beans.factory.annotation.Value;

import java.time.ZonedDateTime;

public class VersionProperties {
    @Value("${GIT_BRANCH}")
    private String gitBranch = "branch";

    @Value("${GIT_HASH}")
    private String gitHash = "000000";

    @Value("${GIT_REF_SLUG}")
    private String gitRefSlug = "LOCAL_DEV";

    @Value("${BUILD_DATE}")
    private ZonedDateTime buildDate = ZonedDateTime.now();

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitHash() {
        return gitHash;
    }

    public String getGitRefSlug() {
        return gitRefSlug;
    }

    public ZonedDateTime getBuildDate() {
        return buildDate;
    }
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
    public String getImplementationTitle() {
        return getClass().getPackage().getImplementationTitle();
    }
}
