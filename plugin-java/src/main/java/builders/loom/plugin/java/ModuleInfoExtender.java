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

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

final class ModuleInfoExtender {

    private static final String MODULE_MAIN_CLASS = "ModuleMainClass";

    private ModuleInfoExtender() {
    }

    static byte[] extend(final byte[] data, final String mainClassName) {
        final ClassWriter cw =
            new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

        try (final AttributeAddingClassVisitor cv =
                 new AttributeAddingClassVisitor(Opcodes.ASM6, cw)) {

            if (mainClassName != null) {
                cv.addAttribute(new ModuleMainClassAttribute(mainClassName));
            }

            final Attribute[] attrs = new Attribute[] {
                new ModuleMainClassAttribute(),
            };

            new ClassReader(data).accept(cv, attrs, 0);
        }

        return cw.toByteArray();
    }

    private static class AttributeAddingClassVisitor extends ClassVisitor implements Closeable {

        private final Map<String, Attribute> attrs = new HashMap<>();

        AttributeAddingClassVisitor(final int api, final ClassVisitor cv) {
            super(api, cv);
        }

        void addAttribute(final Attribute attr) {
            attrs.put(attr.type, attr);
        }

        @Override
        public void visitAttribute(final Attribute attr) {
            final String name = attr.type;

            final Attribute replacement = attrs.get(name);
            if (replacement == null) {
                super.visitAttribute(attr);
                return;
            }

            super.visitAttribute(replacement);
            attrs.remove(name);
        }

        @Override
        public void close() {
            attrs.values().forEach(super::visitAttribute);
            attrs.clear();
        }

    }

    private static class ModuleMainClassAttribute extends Attribute {

        private final String mainClass;

        ModuleMainClassAttribute(final String mainClass) {
            super(MODULE_MAIN_CLASS);
            this.mainClass = mainClass;
        }

        ModuleMainClassAttribute() {
            this(null);
        }

        @Override
        protected Attribute read(final ClassReader cr, final int off, final int len,
                                 final char[] buf, final int codeOff, final Label[] labels) {
            final String mainClassName = cr.readClass(off, buf).replace('/', '.');
            return new ModuleMainClassAttribute(mainClassName);
        }

        @Override
        protected ByteVector write(final ClassWriter cw, final byte[] code, final int len,
                                   final int maxStack, final int maxLocals) {
            final ByteVector v = new ByteVector();
            final int index = cw.newClass(mainClass.replace('.', '/'));
            v.putShort(index);
            return v;
        }
    }

}
