/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.postgresql.codec;

import io.netty.buffer.ByteBuf;
import io.r2dbc.postgresql.client.Parameter;
import org.junit.jupiter.api.Test;

import static io.r2dbc.postgresql.client.Parameter.NULL_VALUE;
import static io.r2dbc.postgresql.client.ParameterAssert.assertThat;
import static io.r2dbc.postgresql.message.Format.FORMAT_BINARY;
import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.BPCHAR;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.BPCHAR_ARRAY;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.CHAR;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.CHAR_ARRAY;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.TEXT;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.TEXT_ARRAY;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.VARCHAR_ARRAY;
import static io.r2dbc.postgresql.util.ByteBufUtils.encode;
import static io.r2dbc.postgresql.util.TestByteBufAllocator.TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class StringArrayCodecTest {

    private final ByteBuf BINARY_ARRAY = TEST
        .buffer()
        .writeInt(1)
        .writeInt(0)
        .writeInt(1043)
        .writeInt(2)
        .writeInt(2)
        .writeInt(3)
        .writeBytes("abc".getBytes())
        .writeInt(3)
        .writeBytes("def".getBytes());

    @Test
    void decodeItem() {
        assertThat(new StringArrayCodec(TEST).decode(BINARY_ARRAY, FORMAT_BINARY, String[].class)).isEqualTo(new String[]{"abc", "def"});
        assertThat(new StringArrayCodec(TEST).decode(encode(TEST, "{alpha,bravo}"), FORMAT_TEXT, String[].class))
            .isEqualTo(new String[]{"alpha", "bravo"});
        assertThat(new StringArrayCodec(TEST).decode(encode(TEST, "{}"), FORMAT_TEXT, String[].class))
            .isEqualTo(new String[]{});
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void decodeObject() {
        assertThat(((Codec) new StringArrayCodec(TEST)).decode(encode(TEST, "{alpha,bravo}"), FORMAT_TEXT, Object.class))
            .isEqualTo(new String[]{"alpha", "bravo"});
    }

    @Test
    void doCanDecode() {
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, BPCHAR)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_BINARY, BPCHAR_ARRAY)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, CHAR)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_BINARY, CHAR_ARRAY)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, CHAR_ARRAY)).isTrue();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, TEXT)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_BINARY, TEXT_ARRAY)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, TEXT_ARRAY)).isTrue();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, VARCHAR)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_BINARY, VARCHAR_ARRAY)).isFalse();
        assertThat(new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, VARCHAR_ARRAY)).isTrue();
    }

    @Test
    void doCanDecodeNoFormat() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringArrayCodec(TEST).doCanDecode(null, VARCHAR_ARRAY))
            .withMessage("format must not be null");
    }

    @Test
    void doCanDecodeNoType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringArrayCodec(TEST).doCanDecode(FORMAT_TEXT, null))
            .withMessage("type must not be null");
    }

    @Test
    void encodeArray() {
        assertThat(new StringArrayCodec(TEST).encodeArray(encode(TEST, "{alpha,bravo}")))
            .hasFormat(FORMAT_TEXT)
            .hasType(TEXT_ARRAY.getObjectId())
            .hasValue(encode(TEST, "{alpha,bravo}"));
    }

    @Test
    void encodeArrayNoByteBuf() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringArrayCodec(TEST).encodeArray(null))
            .withMessage("byteBuf must not be null");
    }

    @Test
    void encodeItem() {
        assertThat(new StringArrayCodec(TEST).encodeItem("alpha")).isEqualTo("\"alpha\"");
    }

    @Test
    void encodeItemMultibyte() {
        assertThat(new StringArrayCodec(TEST).encodeItem("АБ")).isEqualTo("\"АБ\"");
    }

    @Test
    void encodeItemNULL() {
        assertThat(new StringArrayCodec(TEST).encodeItem("NULL")).isEqualTo("\"NULL\"");
    }

    @Test
    void encodeItemNoValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StringArrayCodec(TEST).encodeItem(null))
            .withMessage("value must not be null");
    }

    @Test
    void encodeItemWithEscapes() {
        assertThat(new StringArrayCodec(TEST).encodeItem("R \"2\" DBC")).isEqualTo("\"R \\\"2\\\" DBC\"");
    }

    @Test
    void encodeNull() {
        assertThat(new StringArrayCodec(TEST).encodeNull())
            .isEqualTo(new Parameter(FORMAT_TEXT, TEXT_ARRAY.getObjectId(), NULL_VALUE));
    }

}
