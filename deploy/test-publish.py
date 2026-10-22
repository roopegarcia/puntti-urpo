#!/usr/bin/env python3
"""Publish safety checks in temporary Git repos, with fake SSH/Docker/curl; no production writes."""
import os, subprocess, tempfile, shutil
from pathlib import Path
root = Path(__file__).resolve().parent.parent

def run(args, cwd, env=None):
    return subprocess.run(args, cwd=cwd, env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
with tempfile.TemporaryDirectory(prefix='puntti-publish-test-') as td:
    t = Path(td)
    repo = t / 'repo'
    r = run(['git', 'clone', '--shared', '--quiet', str(root), str(repo)], root)
    assert r.returncode == 0, r.stdout
    for f in ['deploy/publish', 'deploy/compose.yaml']:
        shutil.copy2(root / f, repo / f)

    def commit():
        assert run(['git', 'add', '.'], repo).returncode == 0
        assert run(['git', '-c', 'user.name=Publish Test', '-c', 'user.email=publish-test@example.invalid', 'commit', '--quiet', '-m', 'Fixture only'], repo).returncode == 0
    commit()
    base = run(['git', 'rev-parse', 'HEAD'], repo).stdout.strip()
    fixture_bin = t / 'preflight-bin'
    fixture_bin.mkdir()
    baseline = run(['git', 'rev-parse', 'HEAD'], root).stdout.strip()
    ssh = fixture_bin / 'ssh'
    ssh.write_text('#!/bin/sh\nprintf "puntti-urpo-backend:' + baseline + '\\npuntti-urpo-web:' + baseline + '\\n"\n')
    ssh.chmod(493)
    fixture_env = os.environ.copy()
    fixture_env['PATH'] = str(fixture_bin) + ':' + fixture_env['PATH']
    r = run(['./deploy/publish', '--dry-run'], repo, fixture_env)
    assert r.returncode == 0, r.stdout
    print('PASS: committed preflight with simulated production tags')
    for (f, message) in [('backend/pom.xml', 'Backend changes'), ('deploy/nginx.conf', 'Container/proxy changes'), ('deploy/compose.yaml', 'Compose changes')]:
        with (repo / f).open('a') as h:
            h.write('\n# fixture change\n')
        commit()
        r = run(['./deploy/publish', '--dry-run'], repo, fixture_env)
        assert r.returncode != 0 and message in r.stdout, r.stdout
        assert run(['git', 'reset', '--hard', base], repo).returncode == 0
        print('PASS: rejected ' + f)
    (repo / 'uncommitted.txt').write_text('fixture')
    r = run(['./deploy/publish', '--dry-run'], repo, fixture_env)
    assert r.returncode != 0 and 'clean committed' in r.stdout, r.stdout
    print('PASS: refused uncommitted/untracked files')
    remote = (root / 'deploy/publish').read_text().split("<<'REMOTE'\n", 1)[1].rsplit('\nREMOTE', 1)[0]
    for case in ['success', 'public-failure', 'build-failure', 'concurrent-change']:
        work = t / case
        work.mkdir()
        stage = work / 'staging'
        stage.mkdir()
        (stage / 'deploy').mkdir()
        (stage / 'deploy/compose.yaml').write_text('fixture compose')
        (work / '.env').write_text('RELEASE=abcdef123456\nPOSTGRES_PASSWORD=fixture-secret\n')
        state = work / 'state'
        state.write_text('puntti-urpo-web:abcdef123456')
        bins = work / 'bin'
        bins.mkdir()
        scripts = {'flock': '#!/bin/sh\nexit 0\n', 'docker': '#!/bin/bash\nif [[ $1 == inspect ]]; then\n if [[ ${@: -1} == *backend* ]]; then echo puntti-urpo-backend:abcdef123456\n else cat "$TEST_STATE"; echo; fi\nelse\n echo "$*" >> "$TEST_LOG"\n if [[ " $* " == *" build "* && $TEST_CASE == build-failure ]]; then exit 1; fi\n if [[ " $* " == *" up "* ]]; then printf \'puntti-urpo-web:%s\' "$WEB_RELEASE" > "$TEST_STATE"; fi\nfi\n', 'curl': '#!/bin/bash\nif [[ $TEST_CASE == public-failure && ${@: -1} == https* && $(cat "$TEST_STATE") == *123456789abc ]]; then exit 22; fi\nif [[ " $* " == *" %{http_code} "* ]]; then printf 401; else printf \'<html lang="fi">\'; fi\n'}
        for (name, body) in scripts.items():
            p = bins / name
            p.write_text(body)
            p.chmod(493)
        if case == 'concurrent-change':
            state.write_text('puntti-urpo-web:bbbbbbbbbbbb')
        script = work / 'remote.sh'
        script.write_text(remote.replace('cd /home/roope/apps/puntti-urpo', 'cd ' + str(work)))
        env = os.environ.copy()
        env.update(PATH=str(bins) + ':' + env['PATH'], TEST_STATE=str(state), TEST_LOG=str(work / 'commands'), TEST_CASE=case)
        r = run(['bash', str(script), str(stage), '123456789abc', 'puntti-urpo-backend:abcdef123456', 'puntti-urpo-web:abcdef123456'], work, env)
        if case == 'success':
            assert r.returncode == 0 and state.read_text().endswith('123456789abc'), r.stdout
        else:
            assert r.returncode != 0 and (not state.read_text().endswith('123456789abc')), r.stdout
        assert 'POSTGRES_PASSWORD=fixture-secret' in (work / '.env').read_text()
        commands = (work / 'commands').read_text() if (work / 'commands').exists() else ''
        assert not any((' up ' in line and (not line.endswith(' web')) for line in commands.splitlines()))
        print('PASS: remote ' + case + '; secrets preserved and only web targeted')
