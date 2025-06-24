/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.expression.function.udf.ip;

import java.util.List;
import org.apache.calcite.adapter.enumerable.NotNullImplementor;
import org.apache.calcite.adapter.enumerable.NullPolicy;
import org.apache.calcite.adapter.enumerable.RexToLixTranslator;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.sql.type.*;
import org.opensearch.sql.calcite.utils.PPLOperandTypes;
import org.opensearch.sql.calcite.utils.PPLReturnTypes;
import org.opensearch.sql.data.model.ExprIpValue;
import org.opensearch.sql.expression.function.ImplementorUDF;
import org.opensearch.sql.expression.function.UDFOperandMetadata;
import org.opensearch.sql.utils.IPUtils;

/**
 * <code>ip_format(ip)</code> transfer IP address into new format with zero prefixes
 *
 *
 * <p>Signature:
 *
 * <ul>
 *   <li>STRING -> STRING
 * </ul>
 */
public class IPFormatFunction extends ImplementorUDF {
    public IPFormatFunction() {
        super(new IPFormatImplementor(), NullPolicy.ANY);
    }

    @Override
    public SqlReturnTypeInference getReturnTypeInference() { return PPLReturnTypes.STRING_FORCE_NULLABLE; }

    @Override
    public UDFOperandMetadata getOperandMetadata() {
        // EXPR_IP is mapped to SqlTypeFamily.VARCHAR
        return PPLOperandTypes.STRING;
    }

    public static class IPFormatImplementor implements NotNullImplementor {
        @Override
        public Expression implement(
            RexToLixTranslator translator, RexCall call, List<Expression> translatedOperands) {
            return Expressions.call(IPFormatImplementor.class, "ipFormat", translatedOperands);
        }

        public static String ipFormat(String ip) {
            try {
                return IPUtils.formatIPAddress(ip);
            } catch (Exception e) {
                return ip;
            }
        }

        public static String ipFormat(ExprIpValue ip) {
            return ipFormat(ip.value());
        }
    }
}
