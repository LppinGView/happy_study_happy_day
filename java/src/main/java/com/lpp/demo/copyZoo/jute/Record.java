package com.lpp.demo.copyZoo.jute;

import java.io.IOException;

public interface Record {
    void serialize(OutputArchive archive, String tag) throws IOException;
    void deserialize(InputArchive archive, String tag) throws IOException;
}