/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.kaidad.geo;

import android.os.Bundle;
import android.os.Debug;
import org.apache.cordova.Config;
import org.apache.cordova.DroidGap;

public class Geo extends DroidGap {

    private static final boolean DEBUG = false;
    public static final String TRACE_NAME = "geo";

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) {
            Debug.stopMethodTracing();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Debug.stopMethodTracing();
        }
    }

    @Override
    protected void onResume() {
        if (DEBUG) {
            Debug.startMethodTracing(TRACE_NAME);
        }
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Debug.startMethodTracing(TRACE_NAME);
        }

        super.onCreate(savedInstanceState);
        super.loadUrl(Config.getStartUrl());
    }
}

