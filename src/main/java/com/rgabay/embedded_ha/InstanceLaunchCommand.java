package com.rgabay.embedded_ha;

import java.io.IOException;


/**
 * Created by rossgabay on 4/19/17.
 */
public class InstanceLaunchCommand implements Runnable {
    private final int instanceId;

    public InstanceLaunchCommand(int instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void run() {
        Driver d = new Driver();
        try {
            System.out.printf("trying to launch instance %d \n", instanceId);
            d.createDb(instanceId);
            System.out.printf("instance %d is up\n", instanceId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
