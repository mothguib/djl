/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.pytorch.zoo.cv.objectdetection;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.modality.cv.DetectedObjects;
import ai.djl.modality.cv.SingleShotDetectionTranslator;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.pytorch.zoo.PtModelZoo;
import ai.djl.repository.MRL;
import ai.djl.repository.Repository;
import ai.djl.repository.zoo.BaseModelLoader;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Pipeline;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorFactory;
import ai.djl.util.Progress;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model loader for Single Shot Detection models.
 *
 * <p>The model was trained on PyTorch and loaded in DJL in {@link
 * ai.djl.pytorch.engine.PtSymbolBlock}. See <a href="https://arxiv.org/pdf/1512.02325.pdf">SSD</a>.
 *
 * @see ai.djl.pytorch.engine.PtSymbolBlock
 */
public class SingleShotDetectionModelLoader
        extends BaseModelLoader<BufferedImage, DetectedObjects> {

    private static final Application APPLICATION = Application.CV.OBJECT_DETECTION;
    private static final String GROUP_ID = PtModelZoo.GROUP_ID;
    private static final String ARTIFACT_ID = "ssd";
    private static final String VERSION = "0.0.1";

    /**
     * Creates the Model loader from the given repository.
     *
     * @param repository the repository to load the model from
     */
    public SingleShotDetectionModelLoader(Repository repository) {
        super(repository, MRL.model(APPLICATION, GROUP_ID, ARTIFACT_ID), VERSION);
        Map<Type, TranslatorFactory<?, ?>> map = new ConcurrentHashMap<>();
        map.put(DetectedObjects.class, new FactoryImpl());
        factories.put(BufferedImage.class, map);
    }

    /** {@inheritDoc} */
    @Override
    public Application getApplication() {
        return APPLICATION;
    }

    @Override
    public ZooModel<BufferedImage, DetectedObjects> loadModel(
            Map<String, String> filters, Device device, Progress progress)
            throws IOException, ModelNotFoundException, MalformedModelException {
        Criteria<BufferedImage, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(BufferedImage.class, DetectedObjects.class)
                        .optFilters(filters)
                        .optDevice(device)
                        .optProgress(progress)
                        .build();
        return loadModel(criteria);
    }

    private static final class FactoryImpl
            implements TranslatorFactory<BufferedImage, DetectedObjects> {

        @Override
        public Translator<BufferedImage, DetectedObjects> newInstance(
                Map<String, Object> arguments) {
            int width = ((Double) arguments.getOrDefault("width", 300)).intValue();
            int height = ((Double) arguments.getOrDefault("height", 300)).intValue();
            double threshold = ((Double) arguments.getOrDefault("threshold", 0.4d));

            Pipeline pipeline = new Pipeline();
            pipeline.add(new Resize(width, height))
                    .add(
                            new Normalize(
                                    new float[] {0.485f, 0.456f, 0.406f},
                                    new float[] {0.229f, 0.224f, 0.225f}))
                    .add(new ToTensor());

            return SingleShotDetectionTranslator.builder()
                    .setPipeline(pipeline)
                    .setSynsetArtifactName("classes.txt")
                    .optThreshold((float) threshold)
                    .optRescaleSize(width, height)
                    .build();
        }
    }
}
