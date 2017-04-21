package com.rgabay.embedded_ha.util;

import com.beust.jcommander.Parameter;
import lombok.Data;

/**
 * Created by rossgabay on 4/21/17.
 */
@Data
public class JCommanderSetup {

    @Parameter(names = "-c", description = "config file location")
    private String configFile;

}