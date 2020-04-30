/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;

/**
 * All warnings should be reported as errors.
 */
public class StrictWarningsGuard extends WarningsGuard {

    @Override
    public CheckLevel level(JSError error) {
        return error.level.isOn() ? CheckLevel.ERROR : null;
    }

    @Override
    protected int getPriority() {
        return 100; // applied last
    }
}
