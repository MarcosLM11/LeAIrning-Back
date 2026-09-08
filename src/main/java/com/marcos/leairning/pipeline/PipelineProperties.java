package com.marcos.leairning.pipeline;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@ConfigurationProperties(prefix = PipelineProperties.PREFIX)
public class PipelineProperties {

    public static final String PREFIX = "leairning.pipeline";

    private int chunkSize;
    private Boolean keepSeparator;
    private int minChunkLenght;

}
