package org.sagebionetworks.bridge.researchstack.upload;

import android.content.Context;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.researchstack.backbone.StorageAccess;

public class BridgeDataInput {
  private static Gson gson = new Gson();
  String filename;
  String endDate;
  private Object gsonableObject;
  private Class clazz;
  private String inputFilename;

  public BridgeDataInput(Object gsonableObject, Class clazz, String filename, String endDate) {
    this.gsonableObject = gsonableObject;
    this.clazz = clazz;
    this.filename = filename;
    this.endDate = endDate;
  }

  public BridgeDataInput(String inputFilename, String filename, String endDate) {
    this.inputFilename = inputFilename;
    this.filename = filename;
    this.endDate = endDate;
  }

  public InputStream getInputStream(Context context) throws FileNotFoundException {
    if (gsonableObject != null) {
      return new ByteArrayInputStream(gson.toJson(gsonableObject, clazz).getBytes());
    } else {
      return new ByteArrayInputStream(
          StorageAccess.getInstance().getFileAccess().readData(context, inputFilename));
    }
  }
}
