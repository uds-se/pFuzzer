#include <stdio.h>
#include <inttypes.h>

#define PARSER_MAX_ERRORS    16
#define PARSER_ERROR_LENGTH 256

enum TRACE_VALUE_TYPE
{
    type_value_int,
    type_value_ptr,
    type_value_float,
    type_value_double,
    type_value_array,
    type_value_struct
};

struct TRACE_VALUE
{
    enum TRACE_VALUE_TYPE type;
    size_t size;

    union
    {
        int64_t as_int;
        int64_t as_ptr;
        float   as_float;
        double  as_double;

        struct TRACE_VALUE* as_aggregate;
    };
};

struct TRACE_OPERAND
{
    // TODO add taint to operand
    char* name;

    struct TRACE_VALUE* value;
};

struct TRACE_EVENT
{
    int opcode;

    char* function;
    char* instruction;

    struct TRACE_OPERAND* dest_reg;
    struct TRACE_OPERAND** operands;

    // TODO Do we need that?
    //      Since we have the opcode we can know how many operands there are.
    //      Let see how practical this is once we have a few callers.
    int nb_operands;
};

struct TRACE_PARSER
{
    int (*on_event)(struct TRACE_EVENT* event);

    char errors[PARSER_MAX_ERRORS][PARSER_ERROR_LENGTH];
    int nb_errors;
};

int parse_json_file(struct TRACE_PARSER* parser, FILE* json_file);
int parse_json_str(struct TRACE_PARSER* parser, char* json_str, int json_len);
int parse_json_dump_errors(struct TRACE_PARSER* parser, FILE* file);
