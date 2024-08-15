package dev.truewinter.simofa;

import dev.truewinter.simofa.api.GitCredential;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.api.WebsiteBuild;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class GitFetcher {
    public static void fetch(WebsiteBuild build, GitCredential gitCredential, File tmpInDir,
                             File websiteCacheDir) throws GitAPIException, IOException {
        File gitCacheDir = new File(websiteCacheDir, "git");

        if (gitCacheDir.exists()) {
            build.addLog(new SimofaLog(LogType.INFO, "Using git cache"));
            pull(build, gitCredential, tmpInDir, websiteCacheDir);
        } else {
            clone(build, gitCredential, tmpInDir, websiteCacheDir);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void setCredentials(GitCredential gitCredential, TransportCommand cloneCommand) {
        if (gitCredential != null) {
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    gitCredential.getUsername(),
                    gitCredential.getPassword()
            ));
        }
    }

    private static void clone(WebsiteBuild build, GitCredential gitCredential, File tmpInDir,
                              File websiteCacheDir) throws GitAPIException, IOException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(build.getWebsite().getGitUrl())
                .setBranch(build.getWebsite().getGitBranch())
                .setCloneAllBranches(false)
                .setDepth(1)
                .setDirectory(tmpInDir);

        setCredentials(gitCredential, cloneCommand);
        cloneCommand.call().close();

        if (!Util.isBlank(build.getCacheDir())) {
            if (!websiteCacheDir.exists() && !websiteCacheDir.mkdir()) {
                Simofa.getLogger().warn("Failed to create cache directory for website " + build.getWebsite().getId());
            }

            File gitCacheDir = new File(websiteCacheDir, "git");
            if (!gitCacheDir.exists() && !gitCacheDir.mkdir()) {
                build.addLog(new SimofaLog(LogType.WARN, "Unable to create git cache directory for website " + build.getWebsite().getId()));
            } else {
                FileUtils.copyDirectory(tmpInDir, gitCacheDir);
            }
        }
    }

    private static void pull(WebsiteBuild build, GitCredential gitCredential, File tmpInDir,
                             File websiteCacheDir) throws IOException, GitAPIException {
        File gitCacheDir = new File(websiteCacheDir, "git");
        try (Git git = Git.open(gitCacheDir)) {
            PullCommand pullCommand = git.pull();
            setCredentials(gitCredential, pullCommand);
            pullCommand.call();

            FileUtils.copyDirectory(gitCacheDir, tmpInDir);
        }
    }
}
