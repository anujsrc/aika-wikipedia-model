package org.aika.wikipedia;


import org.aika.Document;
import org.aika.training.MetaNetwork;
import org.aika.wikipedia.config.TestConfig;
import org.aika.wikipedia.model.WikipediaModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MetaModelTest implements InitializingBean {

    private static boolean initialized = false;

    @Autowired
    WikipediaModel model;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!initialized) {
            model.init();
            initialized = true;
        }
    }


    @Test
    public void testSimpleEntity() {
        Document doc = model.aikaModel.createDocument("aaaa bbbbb cccc ddddd eee ");

        model.documentN.addInput(doc, 0, doc.getContent().length());

        model.processEntity(doc, 5, 22, "E-XXX");
        model.processTopic(doc, "T-YYY");

        int i = 0;
        int wordCount = 0;
        for(String w: doc.getContent().split(" ")) {
            int j = i + w.length() + 1;
            model.parseWord(doc, i, j, wordCount++, w);
            i = j;
        }

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        MetaNetwork.train(doc);

        System.out.println(doc.activationsToString(true, true, true));

    }
}
