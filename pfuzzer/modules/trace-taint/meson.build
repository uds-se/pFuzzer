subdir('sources')

conf_data = configuration_data()
conf_data.set('java_jar_dir', java_install_dir)
taint_exec = configure_file(input  : 'scripts/trace-taint',
                            output : 'trace-taint',
                            configuration : conf_data)

install_data(taint_exec, install_mode : 'rwxr-xr-x', install_dir : get_option('bindir'))
