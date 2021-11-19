/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * 文件资源
 * @author Tomato
 * Created on 2021.11.15
 */
public abstract class FileStreamResource implements StreamResource {

    /**
     * 获取具体文件
     * @return 文件
     */
    public File getFile() {
        URI uri = URI.create(getURL().toString());
        return new File(uri.getSchemeSpecificPart());
    }


    @Override
    public InputStream openNewStream() throws IOException {
        return getURL().openStream();
    }

    @Override
    public boolean exists() {
        return getFile().exists();
    }

    @Override
    public boolean isReadable() {
        // todo 实现
        return false;
    }
}
