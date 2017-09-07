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

public final class TaskResult {

    private final TaskStatus status;
    private final Product product;
    private final String errorReason;

    private TaskResult(final TaskStatus status, final Product product, final String errorReason) {
        this.status = status;
        this.product = product;
        this.errorReason = errorReason;
    }

    public static TaskResult ok(final Product product) {
        return new TaskResult(TaskStatus.OK, Objects.requireNonNull(product), null);
    }

    public static TaskResult empty() {
        return new TaskResult(TaskStatus.EMPTY, null, null);
    }

    public static TaskResult fail(final Product product, final String errorReason) {
        return new TaskResult(TaskStatus.FAIL, product, errorReason);
    }

    public static TaskResult up2date(final Product product) {
        return new TaskResult(TaskStatus.UP_TO_DATE, product, null);
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Product getProduct() {
        return product;
    }

    public String getErrorReason() {
        return errorReason;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TaskResult{");
        sb.append("status=").append(status);
        sb.append(", product=").append(product);
        if (errorReason != null) {
            sb.append(", errorReason='").append(errorReason).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

}
