package com.solomonj.ipynbviewerpro;

public interface PostResponseCallback {
    void onResponse(String response);
    void onError(Exception e);
}


