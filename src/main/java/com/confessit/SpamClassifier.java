package com.confessit;

import java.io.*;

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;

import weka.core.Instances;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.converters.ArffSaver;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.tokenizers.NGramTokenizer;

import weka.filters.unsupervised.attribute.StringToWordVector;


/**
 * This class implements a Multinomial NaiveBayes text classifier using WEKA.
 * @author Alfred Francis - https://alfredfrancis.github.io
 * @see <a href="http://weka.wikispaces.com/Programmatic+Use">...</a>
 */
public class SpamClassifier {

    private static Logger LOGGER = Logger.getLogger("WekaClassifier");

    private FilteredClassifier classifier;

    //declare train and test data Instances
    private Instances trainData;


    //declare attributes of Instance
    private ArrayList < Attribute > wekaAttributes;

    //declare and initialize file locations
    private static final String TRAIN_DATA = "C:\\Users\\Freyr\\IdeaProjects\\Confess-It\\src\\main\\java\\com\\confessit\\Dataset\\train.txt";
    private static final String TRAIN_ARFF_ARFF = "C:\\Users\\Freyr\\IdeaProjects\\Confess-It\\src\\main\\java\\com\\confessit\\Dataset\\train.arff";
    private static final String TEST_DATA = "C:\\Users\\Freyr\\IdeaProjects\\Confess-It\\src\\main\\java\\com\\confessit\\Dataset\\test.arff";
    private static final String TEST_DATA_ARFF = "C:\\Users\\Freyr\\IdeaProjects\\Confess-It\\src\\main\\java\\com\\confessit\\Dataset\\test.arff";

    SpamClassifier() {

        /*
         * Class for running an arbitrary classifier on data that has been passed through an arbitrary filter
         * Training data and test instances will be processed by the filter without changing their structure
         */
        classifier = new FilteredClassifier();

        // set Multinomial NaiveBayes as arbitrary classifier
        classifier.setClassifier(new NaiveBayesMultinomial());

        // Declare text attribute to hold the message
        Attribute attributeText = new Attribute("text", (List < String > ) null);

        // Declare the label attribute along with its values
        ArrayList < String > classAttributeValues = new ArrayList < > ();
        classAttributeValues.add("spam");
        classAttributeValues.add("ham");
        Attribute classAttribute = new Attribute("label", classAttributeValues);

        // Declare the feature vector
        wekaAttributes = new ArrayList < > ();
        wekaAttributes.add(classAttribute);
        wekaAttributes.add(attributeText);

    }

    /**
     * load training data and set feature generators
     */
    public void transform() {
        try {
            trainData = loadRawDataset(TRAIN_DATA);
            saveArff(trainData, TRAIN_ARFF_ARFF);

            // create the filter and set the attribute to be transformed from text into a feature vector (the last one)
            StringToWordVector filter = new StringToWordVector();
            filter.setAttributeIndices("last");

            //add ngram tokenizer to filter with min and max length set to 1
            NGramTokenizer tokenizer = new NGramTokenizer();
            tokenizer.setNGramMinSize(1);
            tokenizer.setNGramMaxSize(1);
            //use word delimeter
            tokenizer.setDelimiters("\\W");
            filter.setTokenizer(tokenizer);

            //convert tokens to lowercase
            filter.setLowerCaseTokens(true);

            //add filter to classifier
            classifier.setFilter(filter);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }


    }

    /**
     * build the classifier with the Training data
     */
    public void fit() {
        try {
            classifier.buildClassifier(trainData);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
    }


    /**
     * classify a new message into spam or ham.
     * @param text  to be classified.
     * @return a class label (spam or ham )
     */
    public String predict(String text) {
        try {
            // create new Instance for prediction.
            DenseInstance newinstance = new DenseInstance(2);

            //weka demand a dataset to be set to new Instance
            Instances newDataset = new Instances("predictiondata", wekaAttributes, 1);
            newDataset.setClassIndex(0);

            newinstance.setDataset(newDataset);

            // text attribute value set to value to be predicted
            newinstance.setValue(wekaAttributes.get(1), text);

            // predict most likely class for the instance
            double pred = classifier.classifyInstance(newinstance);

            // return original label
            return newDataset.classAttribute().value((int) pred);
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    /**
     * evaluate the classifier with the Test data
     * @return evaluation summary as string
     */
    public String evaluate() {
        try {
            //load testdata
            Instances testData;
            if (new File(TEST_DATA_ARFF).exists()) {
                testData = loadArff(TEST_DATA_ARFF);
                testData.setClassIndex(0);
            } else {
                testData = loadRawDataset(TEST_DATA);
                saveArff(testData, TEST_DATA_ARFF);
            }

            Evaluation eval = new Evaluation(testData);
            eval.evaluateModel(classifier, testData);
            return eval.toSummaryString();
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    /**
     * This method loads the model to be used as classifier.
     * @param fileName The name of the file that stores the text.
     */
    public void loadModel(String fileName) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
            Object tmp = in .readObject();
            classifier = (FilteredClassifier) tmp; in .close();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    /**
     * This method saves the trained model into a file. This is done by
     * simple serialization of the classifier object.
     * @param fileName The name of the file that will store the trained model.
     */

    public void saveModel(String fileName) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
        out.writeObject(classifier);
        out.close();
        System.out.println("Here");
        System.out.println(fileName);
        LOGGER.info("Saved model: " + fileName);
    }

    /**
     * Loads a dataset in space seperated text file and convert it to Arff format.
     * @param filename The name of the file.
     */
    public Instances loadRawDataset(String filename) {
        /*
         *  Create an empty training set
         *  name the relation “Rel”.
         *  set intial capacity of 10*
         */

        Instances dataset = new Instances("SMS spam", wekaAttributes, 10);

        // Set class index
        dataset.setClassIndex(0);

        // read text file, parse data and add to instance
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for (String line;
                 (line = br.readLine()) != null;) {
                // split at first occurance of n no. of words
                String[] parts = line.split("\\s+", 2);

                // basic validation
                if (!parts[0].isEmpty() && !parts[1].isEmpty()) {

                    DenseInstance row = new DenseInstance(2);
                    row.setValue(wekaAttributes.get(0), parts[0]);
                    row.setValue(wekaAttributes.get(1), parts[1]);

                    // add row to instances
                    dataset.add(row);
                }

            }

        }
        catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
        catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.info("invalid row.");
        }
        return dataset;

    }

    /**
     * Loads a dataset in ARFF format. If the file does not exist, or
     * it has a wrong format, the attribute trainData is null.
     * @param fileName The name of the file that stores the dataset.
     */
    public Instances loadArff(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            ArffReader arff = new ArffReader(reader);
            Instances dataset = arff.getData();
            // replace with logger System.out.println("loaded dataset: " + fileName);
            reader.close();
            return dataset;
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
            return null;
        }
    }

    /**
     * This method saves a dataset in ARFF format.
     * @param dataset dataset in arff format
     * @param filename  The name of the file that stores the dataset.
     */
    public void saveArff(Instances dataset, String filename) {
        try {
            // initialize
            ArffSaver arffSaverInstance = new ArffSaver();
            arffSaverInstance.setInstances(dataset);
            arffSaverInstance.setFile(new File(filename));
            arffSaverInstance.writeBatch();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    /**
     * Main method. With an example usage of this class.
     */
    public static void main(String[] args) throws Exception {

        String MODEL = "src/main/resources/com/models/sms.dat";
        SpamClassifier wt = new SpamClassifier();
        wt.loadModel(MODEL);
        System.out.println(wt.predict("Good Morning to you."));




//        if (new File(MODEL).exists()) {
//            wt.loadModel(MODEL);
//        } else {
//            System.out.println("Here");
//            wt.transform();
//            wt.fit();
//            wt.saveModel(MODEL);
//        }

//        //run few predictions
//        LOGGER.info("text 'how are you' is " + wt.predict("how are you ?"));
//        LOGGER.info("text 'u have won the 1 lakh prize' is " + wt.predict("u have won the 1 lakh prize"));
//
//        //run evaluation
//        LOGGER.info("Evaluation Result: \n"+wt.evaluate());
    }
}
