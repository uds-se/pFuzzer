project('checksum-repair', ['c', 'cpp', 'java'],
        version : '0.1.0',
        meson_version : '>= 0.46')

lint_java = find_program('scripts/static_analysis_java.py')
run_target('lint-java', command: [lint_java, meson.source_root()])

subdir('modules/trace-core')
subdir('modules/trace-instr')
subdir('modules/trace-taint')
subdir('modules/constraints-build')
subdir('modules/constraints-repair')

install_subdir('samples', install_dir : join_paths(get_option('datadir'), meson.project_name()))
