#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include <json-c/json_object.h>
#include <json-c/json_tokener.h>

#include <trace.h>

#define BUFSIZE 2048

#define PARSER_ERROR -1

#define FIELD_MANDATORY 0
#define FIELD_OPTIONAL  1

void parser_error(struct TRACE_PARSER* parser, const char* format, ...)
{
    if (parser->nb_errors >= PARSER_MAX_ERRORS)
    {
        return;
    }

    va_list ap;
    va_start(ap, format);
    vsnprintf(parser->errors[parser->nb_errors++], PARSER_ERROR_LENGTH, format, ap);
    va_end(ap);
}

int parse_json_object_int(struct TRACE_PARSER* parser, json_object* json_obj, const char* key, int optional)
{
    json_object* json_value;

    if (!json_object_object_get_ex(json_obj, key, &json_value))
    {
        if (!optional)
        {
            parser_error(parser, "missing required field '%s'", key);
            return 0;
        }

        return 0;
    }

    if (json_object_get_type(json_value) != json_type_int)
    {
        parser_error(parser, "wrong type for field '%s' (expected int)", key);
        return 0;
    }

    return json_object_get_int(json_value);
}

char* parse_json_object_string(struct TRACE_PARSER* parser, json_object* json_obj, const char* key, int optional)
{
    json_object* json_value;

    if (!json_object_object_get_ex(json_obj, key, &json_value))
    {
        if (!optional)
        {
            parser_error(parser, "missing required field '%s'", key);
            return NULL;
        }

        return NULL;
    }

    if (json_object_get_type(json_value) != json_type_string)
    {
        parser_error(parser, "wrong type for field '%s' (expected string)", key);
        return NULL;
    }

    return strdup(json_object_get_string(json_value));
}

struct TRACE_OPERAND* parse_json_operand(struct TRACE_PARSER* parser, json_object* json_operand)
{
    if (json_object_get_type(json_operand) != json_type_object)
    {
        return NULL;
    }

    struct TRACE_OPERAND* operand = malloc(sizeof(struct TRACE_OPERAND));
    operand->name = parse_json_object_string(parser, json_operand, "name", FIELD_MANDATORY);

    if (parser->nb_errors != 0)
    {
        free(operand->name);
        free(operand);
        return NULL;
    }

    return operand;
}

int parse_json_event(struct TRACE_PARSER* parser, struct TRACE_EVENT* event, json_object* json_event)
{
    if (json_object_get_type(json_event) != json_type_object)
    {
        parser_error(parser, "wrong type for event (expect JSON object)");
        return PARSER_ERROR;
    }

    event->opcode = parse_json_object_int(parser, json_event, "opcode", FIELD_MANDATORY);
    event->function = parse_json_object_string(parser, json_event, "function", FIELD_MANDATORY);
    event->instruction = parse_json_object_string(parser, json_event, "instruction", FIELD_OPTIONAL);

    json_object* json_dest;
    if (!json_object_object_get_ex(json_event, "dest", &json_dest))
    {
        parser_error(parser, "missing required event field 'dest'");
        return PARSER_ERROR;
    }

    event->dest_reg = parse_json_operand(parser, json_dest);

    json_object* json_operands;
    if (!json_object_object_get_ex(json_event, "operands", &json_operands))
    {
        parser_error(parser, "missing required event field 'operands'");
        return PARSER_ERROR;
    }

    if (json_object_get_type(json_operands) != json_type_array)
    {
        parser_error(parser, "wrong type for event field 'operands' (expected JSON array)");
        return PARSER_ERROR;
    }

    size_t nb_operands = json_object_array_length(json_operands);
    event->operands = malloc(sizeof(struct TRACE_OPERAND) * nb_operands);
    for (size_t i = 0; i < nb_operands; ++i)
    {
        event->operands[i] = parse_json_operand(parser, json_object_array_get_idx(json_operands, i));
    }

    if (parser->nb_errors != 0)
    {
        parser_error(parser, "error while parsing event");
        free(event->operands);
        return PARSER_ERROR;
    }

    return 0;
}

int parse_json_str(struct TRACE_PARSER* parser, char* json_str, int json_len)
{
    json_tokener* json_tok = json_tokener_new();
    json_object*  json_obj;
    enum json_tokener_error json_err = json_tokener_success;

    // read as many objects as possible from this chunk
    int offset = 0;
    do
    {
        json_obj = json_tokener_parse_ex(json_tok, json_str + offset, json_len - offset);
        json_err = json_tokener_get_error(json_tok);

        if (json_err == json_tokener_success)
        {
            struct TRACE_EVENT event;
            if (parse_json_event(parser, &event, json_obj) != 0)
            {
                return PARSER_ERROR;
            }

            if (parser->on_event != NULL)
            {
                parser->on_event(&event);
            }

            offset += json_tok->char_offset;
        }
    }
    while (json_err == json_tokener_success && offset < json_len);

    return json_err;
}

int parse_json_file(struct TRACE_PARSER* parser, FILE* json_file)
{
    char json_str[BUFSIZE];
    int  json_len;

    int json_err = json_tokener_success;

    while ((json_len = fread(json_str, sizeof(char), BUFSIZE, json_file)) != 0)
    {
        json_err = parse_json_str(parser, json_str, json_len);

        if (json_err != json_tokener_success && json_err != json_tokener_continue)
        {
            if (json_err > 0)
            {
                parser_error(parser, "JSON error: %s", json_tokener_error_desc(json_err));
            }

            return json_err;
        }
    }

    if (json_err != json_tokener_success)
    {
        if (json_err == json_tokener_continue)
        {
            parser_error(parser, "trace seems truncated: unterminated JSON");
        }
        else
        {
            if (json_err > 0)
            {
                parser_error(parser, "JSON error: %s", json_tokener_error_desc(json_err));
            }
        }

        return json_err;
    }

    return 0;
}

int parse_json_dump_errors(struct TRACE_PARSER* parser, FILE* file)
{
    for (int i = 0; i < parser->nb_errors; ++i)
    {
        fprintf(file, "error: %s\n", parser->errors[i]);
    }

    return parser->nb_errors;
}
