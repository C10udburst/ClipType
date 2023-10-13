package io.github.cloudburst.cliptype;

interface IUsbRootService {
    boolean hidCapable();
    void typeUsb(String text);
}