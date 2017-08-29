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

package builders.loom.api;

import java.util.Objects;

import builders.loom.api.product.Product;

public class TaskResult {

    private final TaskStatus status;
    private final Product product;

    public TaskResult(final TaskStatus status, final Product product) {
        if (status == TaskStatus.EMPTY) {
            throw new IllegalArgumentException("TaskResult with product must not EMPTY");
        }
        this.status = Objects.requireNonNull(status);
        this.product = Objects.requireNonNull(product);
    }

    public TaskResult() {
        this.status = TaskStatus.EMPTY;
        this.product = null;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    public String toString() {
        return "TaskResult{"
            + "status=" + status
            + ", product=" + product
            + '}';
    }

}
