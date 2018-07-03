package com.gitee.jenkins.service;


import com.gitee.jenkins.gitee.api.model.Label;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.gitee.jenkins.gitee.api.model.builder.generated.LabelBuilder.label;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class GiteeProjectLabelsServiceTest {

    private final static List<String> LABELS_PROJECT_B = asList("label1", "label2", "label3");

    private GiteeProjectLabelsService labelsService;

    private GiteeClientStub clientStub;

    @Before
    public void setUp() throws IOException {
        clientStub = new GiteeClientStub();
        clientStub.addLabels("groupOne/A", convert(asList("label1", "label2")));
        clientStub.addLabels("groupOne/B", convert(LABELS_PROJECT_B));

        // never expire cache for tests
        labelsService = new GiteeProjectLabelsService();
    }

    @Test
    public void shouldReturnLabelsFromGiteeApi() {
        // when
        List<String> actualLabels = labelsService.getLabels(clientStub, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualLabels, is(LABELS_PROJECT_B));
    }

    @Test
    public void shouldNotMakeUnnecessaryCallsToGiteeApiGetLabels() {
        // when
        labelsService.getLabels(clientStub, "git@git.example.com:groupOne/A.git");

        // then
        assertEquals(1, clientStub.calls("groupOne/A", Label.class));
        assertEquals(0, clientStub.calls("groupOne/B", Label.class));
    }

    private List<Label> convert(List<String> labels) {
        ArrayList<Label> result = new ArrayList<>();
        for (String label : labels) {
            result.add(label().withName(label).build());
        }
        return result;
    }
}
