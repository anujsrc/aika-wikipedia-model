package org.aika.wikipedia;

import org.aika.Model;
import org.aika.Provider;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.storage.MongoSuspensionHook;
import org.aika.wikipedia.importer.Annotation;
import org.aika.wikipedia.importer.WikipediaImporter;
import org.aika.wikipedia.importer.impl.WikiArticle;
import org.aika.wikipedia.model.WikipediaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableMongoRepositories(basePackages = {"org.aika.storage"})
public class Importer implements CommandLineRunner {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    Model aikaModel;

    @Autowired
    WikipediaModel wikiModel;

    @Autowired
    Train train;

    @Autowired
    MongoSuspensionHook suspensionHook;


    public static void main(String[] args) {
        SpringApplication.run(Importer.class, args);
    }


    public static String getText(String txt, Range r) {
        return txt.substring(Math.max(0, r.begin), Math.min(txt.length(), r.end));
    }


    @Override
    public void run(String... args) throws Exception {
        suspensionHook.deleteAll();

        wikiModel.init();

        WikipediaImporter importer = new WikipediaImporter(args[0]);

        for(String[] docRaw: importer.getDocuments()) {
            WikiArticle wikiDoc = new WikiArticle(docRaw[0], docRaw[1]);
            wikiDoc.parse();
//            String keyword = getText(wikiDoc.text.toString(), wikiDoc.keywordAnnotation.r);

            log.info("Title:" + wikiDoc.title + "\n");

            Document doc = map(wikiDoc);

            train.train(doc);

            doc.clearActivations();
        }


        System.out.println("Suspend All");
        aikaModel.suspendAll(Provider.SuspensionMode.SAVE);
    }


    private Document map(WikiArticle wikiDoc) {
        String text = wikiDoc.text.toString();
        text = text.replaceAll("\n", " ");
        Document doc = aikaModel.createDocument(text);

        try {
            wikiModel.documentN.addInput(doc, 0, doc.length());

            for (Annotation anno : wikiDoc.annotations) {
                for (String attr : anno.attributes) {

                    if(attr.startsWith("E-")) {
                        int b = anno.r.begin;
                        int e = (anno.r.end < doc.length() && doc.getContent().charAt(anno.r.end) == ' ') ? anno.r.end + 1 : anno.r.end;

                        wikiModel.processEntity(doc, b, e, attr);
                    }

                    if(attr.equals("ARTICLE-KEYWORD")) {
                        for(String a: anno.attributes) {
                            if (a.startsWith("E-") || a.startsWith("C-")) {
                                wikiModel.processTopic(doc, "T-" + a.substring(2, a.length()));
                            }
                        }
                    }
                }
            }

            int i = 0;
            int wordPos = 0;
            for (String w : doc.getContent().split(" |\\(|\\)|\\#|/|,|'|\\[|\\]|\\<|\\>|„|“|\\.|;|:|-|\\?|!|\\p{Z}")) {
                int j = i + w.length();

                if (i != j) {

                    // Nimm das Space Zeichen nach dem Wort noch mit rein.
                    int end = jumpToEndOfSpace(doc, j);

                    wikiModel.parseWord(doc, i, end, wordPos, w);
/*  TODO: Abkürzungen implementieren
                    // Matche Abkürzungen
                    int alternativeEnd = jumpToEndOfSpace(doc, doc.getContent().indexOf(' ', i));
                    if (end != alternativeEnd) {
                        String alternativeWord = doc.getContent().substring(i, end + 1);
                        parseWord(doc, i, alternativeEnd, wordPos, alternativeWord);
                    }
*/
                    wordPos++;
                }
                i = j + 1;
            }

            log.info("Finished adding input activations");
            log.info("Start search for best interpretation");
            doc.process();
            log.info("Finished search for best interpretation");

            log.info(doc.bestInterpretationToString());

            log.info(doc.activationsToString(true, false));
        } catch (Exception e) {
            doc.clearActivations();
            throw e;
        }


        return doc;
    }


    public String extractPhrase(String phraseKey) {
        int brPos = phraseKey.indexOf('(');
        if(brPos >= 0) {
            phraseKey = phraseKey.substring(0, brPos).trim();
        }
        return phraseKey.substring(2);
    }


    public static int jumpToEndOfSpace(Document doc, int j) {
        if(j < 0) {
            j = doc.getContent().length();
        }

        while (j < doc.getContent().length() && doc.getContent().charAt(j) == ' ') {
            j++;
        }
        return j;
    }
}