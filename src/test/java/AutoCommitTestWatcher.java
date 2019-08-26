import java.io.*;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public class AutoCommitTestWatcher implements TestWatcher {

    static class Status {

        private String id;
        private boolean done;

        // dummy class to easily parse done.json
        public Status() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean getDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    // count the number of tests that have passed
    private int cpt;

    // return true when all tests have passed
    // ie, cpt equals to "nbTest" declared in the test class
    private boolean allTestsHavePassed(int cpt, ExtensionContext extensionContext) {
        return (cpt == Integer.parseInt(extensionContext.getConfigurationParameter("nbTest").get()));
    }

    // convert done.json into a List of Status
    private List<Status> fetchStatusOfAllQuestions(ExtensionContext extensionContext) {
        ObjectMapper mapper = new ObjectMapper();
        List<Status> questions = null;
        try {
            questions = Arrays.asList(mapper.readValue(new FileReader("src/main/resources/done.json"), Status[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questions;
    }

    // return an object Status corresponding to the entry in done.json whose id is specified
    // with the string "question" in the test class
    private Status getStatusOfTheQuestion(ExtensionContext extensionContext, List<Status> questions) {
        String key = extensionContext.getConfigurationParameter("question").get();

        return questions.stream()
                .filter(s -> key.equals(s.getId()))
                .findFirst()
                .orElse(null);

    }

    // update (from false to true) the status of the entry in done.json whose id is specified
    // with the string "question" in the test class
    private void updateStatusOfTheQuestion(ExtensionContext extensionContext) {
        List<Status> questions = fetchStatusOfAllQuestions(extensionContext);
        Status status = getStatusOfTheQuestion(extensionContext, questions);
        status.setDone(true);

        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(new File("src/main/resources/done.json"), questions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        try {
            // get the local repository
            Repository repository = repositoryBuilder.setGitDir(new File(System.getProperty("user.dir") + "\\.git"))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .setMustExist(true)
                    .build();
            Git git = new Git(repository);

            // commit now!
            RevCommit commit = git.commit()
                    .setMessage(extensionContext.getConfigurationParameter("question").get() + " done")
                    .call();

            // get the access token
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = null;
            try {
                rootNode = objectMapper.readTree(new FileReader("src/main/resources/config.json"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String token = rootNode.get("token").asText();
            System.out.println("--> " + token);

            // push it!
            PushCommand push = git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .setRemote("origin")
                    .add("master");
            push.call();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // return true when the status of the entry in done.json whose id is specified
    // with the string "question" in the test class is false
    private boolean isMarkedAsNotCompleted(ExtensionContext extensionContext) {
        List<Status> questions = fetchStatusOfAllQuestions(extensionContext);
        return !getStatusOfTheQuestion(extensionContext, questions).getDone();
    }

    // update the number of tests that have passed and update done.json the first time all tests have passed
    // and provoke a commit
    @Override
    public void testSuccessful(ExtensionContext extensionContext) {
        cpt++;
        if (allTestsHavePassed(cpt, extensionContext) && isMarkedAsNotCompleted(extensionContext))
            updateStatusOfTheQuestion(extensionContext);
    }
}