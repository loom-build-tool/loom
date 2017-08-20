/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.plugin.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

final class ModuleInfoExtender {

    private ModuleInfoExtender() {
    }

    static byte[] extend(final byte[] data, final String mainClassName) {
        Objects.requireNonNull(data, "data required");
        Objects.requireNonNull(mainClassName, "mainClassName required");

        final ClassWriter cw =
            new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

        final List<Attribute> attributes = List.of(new ModuleMainClassAttribute(mainClassName));

        final AttributeAddingClassVisitor cv =
            new AttributeAddingClassVisitor(Opcodes.ASM6, cw, attributes);

        new ClassReader(data).accept(cv, 0);

        attributes.forEach(cv::visitAttribute);

        return cw.toByteArray();
    }

    private static class AttributeAddingClassVisitor extends ClassVisitor {

        private final Map<String, Attribute> attrs = new HashMap<>();

        AttributeAddingClassVisitor(final int api, final ClassVisitor cv,
                                    final List<Attribute> attributes) {
            super(api, cv);
            attributes.forEach(a -> attrs.put(a.type, a));
        }

        @Override
        public void visitAttribute(final Attribute attr) {
            super.visitAttribute(attrs.getOrDefault(attr.type, attr));
        }

    }

    private static class ModuleMainClassAttribute extends Attribute {

        private static final String MODULE_MAIN_CLASS = "ModuleMainClass";

        private final String mainClassName;

        ModuleMainClassAttribute(final String mainClassName) {
            super(MODULE_MAIN_CLASS);
            this.mainClassName = mainClassName;
        }

        @Override
        protected ByteVector write(final ClassWriter cw, final byte[] code, final int len,
                                   final int maxStack, final int maxLocals) {
            final ByteVector v = new ByteVector();
            final int index = cw.newClass(mainClassName.replace('.', '/'));
            v.putShort(index);
            return v;
        }

    }

}
