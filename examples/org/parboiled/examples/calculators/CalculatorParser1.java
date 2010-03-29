/*
 * Copyright (C) 2009 Mathias Doenitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parboiled.examples.calculators;

import org.parboiled.Rule;

import java.util.List;

import static java.lang.Integer.parseInt;
import static org.parboiled.common.StringUtils.isEmpty;

public class CalculatorParser1 extends CalculatorParser<Integer> {

    @Override
    public Rule inputLine() {
        return sequence(expression(), eoi());
    }

    public Rule expression() {
        return sequence(
                term(),
                zeroOrMore(
                        sequence(
                                charSet("+-"),
                                term()
                        )
                ),
                // "z/s/[" is short for "zeroOrMore/sequence/[+-]"
                compute(VALUE("term"), CHARS("z/s/["), VALUES("z/s/term"))
        );
    }

    public Rule term() {
        return sequence(
                factor(),
                zeroOrMore(
                        sequence(
                                charSet("*/"),
                                factor()
                        )
                ),
                // "z/s/[" is short for "zeroOrMore/sequence/[*/]"
                compute(VALUE("factor"), CHARS("z/s/["), VALUES("z/s/factor"))
        );
    }

    public Rule factor() {
        return firstOf(
                number(),
                parens()
        );
    }

    public Rule parens() {
        return sequence('(', expression(), ')');
    }

    public Rule number() {
        return sequence(
                oneOrMore(digit()),
                SET(isEmpty(LAST_TEXT()) ? 0 : parseInt(LAST_TEXT()))
        );
    }

    public Rule digit() {
        return charRange('0', '9');
    }

    //**************** ACTIONS ****************

    public boolean compute(Integer firstValue, List<Character> operators, List<Integer> values) {
        int value = firstValue != null ? firstValue : 0;
        for (int i = 0; i < Math.min(operators.size(), values.size()); i++) {
            value = performOperation(value, operators.get(i), values.get(i));
        }
        return SET(value);
    }

    private int performOperation(int value1, Character operator, Integer value2) {
        if (operator != null && value2 != null) {
            switch (operator) {
                case '+':
                    return value1 + value2;
                case '-':
                    return value1 - value2;
                case '*':
                    return value1 * value2;
                case '/':
                    return value1 / value2;
            }
        }
        throw new IllegalStateException();
    }

    //**************** MAIN ****************

    public static void main(String[] args) {
        main(CalculatorParser1.class);
    }

}