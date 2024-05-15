package io.github.cloudburst.cliptype;

interface IUsbRootService {
    String devPath();
    boolean gadgetExists();
    boolean canWrite();
    List<String> enabledGadgets();
    void typeUsb(String text);
}