tracecore_sources = '''
    sources/parse_json.c
    sources/parse_msgpack.c
'''.split()

JSONc_dep = dependency('json-c', version : '0.13.1')

tracecore_inc = include_directories('include')
tracecore_lib = library('tracecore',
                        tracecore_sources,
                        include_directories : tracecore_inc,
                        dependencies : JSONc_dep,
                        install : true)

traceparser_test = executable('traceparser_test',
                              'tests/traceparser_test.c',
                              include_directories : tracecore_inc,
                              link_with : tracecore_lib)

test('JSON parse empty trace', traceparser_test, args : files('tests/empty.json'))
test('JSON parse one event', traceparser_test, args : files('tests/one.json'))
test('JSON parse multiple events', traceparser_test, args : files('tests/multi.json'))
test('JSON truncated trace should fail', traceparser_test, args : files('tests/truncated.json'), should_fail : true)
test('JSON non-event json should fail', traceparser_test, args : files('tests/not_event.json'), should_fail : true)
