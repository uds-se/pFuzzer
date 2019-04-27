llvm_dep = dependency('llvm', version : ['>=4.0', '<4.1'], modules : ['core'])

traceinstr_inc = include_directories('include')

traceplugin = shared_library('traceplugin',
                             ['sources/traceplugin.cpp'],
                             dependencies        : llvm_dep,
                             include_directories : [traceinstr_inc, include_directories(llvm_dep.get_configtool_variable('includedir'))],
                             cpp_args            : llvm_dep.get_configtool_variable('cxxflags').split(),
                             link_args           : llvm_dep.get_configtool_variable('ldflags').split(),
                             install             : true)

simpletracer = static_library('simpletracer',
                              ['sources/tracers/simpletracer.c'],
                              include_directories : traceinstr_inc,
                              dependencies        : JSONc_dep,
                              install             : true)

wrappermain = static_library('wrappermain',
                             ['sources/wrappers/main.c'],
                             include_directories : [traceinstr_inc, include_directories(llvm_dep.get_configtool_variable('includedir'))],
                             install             : true)

wrapperlibc = static_library('wrapperlibc',
                             ['sources/wrappers/libc.c'],
                             include_directories : [traceinstr_inc, include_directories(llvm_dep.get_configtool_variable('includedir'))],
                             install             : true)

conf_data = configuration_data()
conf_data.set('LLVM_BINDIR', llvm_dep.get_configtool_variable('bindir'))
conf_data.set('CLANG', join_paths(llvm_dep.get_configtool_variable('bindir'), 'clang'))
conf_data.set('TRACEPLUGIN', traceplugin.full_path())
conf_data.set('LIBDIR', get_option('libdir'))
conf_data.set('INCDIR', get_option('includedir'))
trace_exec = configure_file(input  : 'scripts/trace-instr',
                            output : 'trace-instr',
                            configuration : conf_data)

wraplibc_header = custom_target('wraplibc_header',
  output       : 'wrapper_libc.h',
  input        : 'include/wrapper_libc.h.in',
  command      : ['python3',  join_paths(meson.current_source_dir(), 'scripts/wrapcall.py'), '@INPUT@', '@OUTPUT@'],
  depend_files : 'scripts/wrapcall.py',
  install      : true,
  install_dir  : join_paths(get_option('includedir'), 'traceinstr'))

install_data(trace_exec, install_mode : 'rwxr-xr-x', install_dir : get_option('bindir'))
install_data('scripts/extract_metadata', install_mode : 'rwxr-xr-x', install_dir : get_option('bindir'))
