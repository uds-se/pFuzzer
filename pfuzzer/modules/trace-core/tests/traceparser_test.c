#include <stdio.h>
#include <trace.h>

int main(int argc, char* argv[])
{
    if (argc != 2)
    {
        fprintf(stderr, "error: usage: %s <json_file>\n", argv[0]);
        return 1;
    }

    FILE* json_file = fopen(argv[1], "r");

    struct TRACE_PARSER parser = { .on_event = NULL };
    int res = parse_json_file(&parser, json_file);

    if (res != 0)
    {
        parse_json_dump_errors(&parser, stderr);
    }

    return res;
}
