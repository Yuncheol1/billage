import subprocess
import os
import argparse
import sys
import time

def run_command(command, cwd=None):
    """쉘 명령어를 실행하고 결과를 출력합니다."""
    print(f"Executing: {command}")
    process = subprocess.Popen(
        command,
        shell=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        cwd=cwd,
        text=True
    )
    for line in process.stdout:
        print(line, end="")
    process.wait()
    return process.returncode

def main():
    parser = argparse.ArgumentParser(description="Jenkins 통합 성능 테스트 실행기")
    parser.add_argument("--host", default="http://localhost:8000", help="대상 서버 URL")
    parser.add_argument("--users", type=int, default=10, help="동시 유저 수")
    parser.add_argument("--spawn-rate", type=int, default=2, help="초당 유저 생성 수")
    parser.add_argument("--run-time", default="1m", help="테스트 실행 시간 (예: 1m, 30s)")
    args = parser.parse_args()

    # 1. 설정 확인 및 디렉토리 준비
    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    perf_dir = os.path.join(project_root, "tests", "performance")
    report_dir = os.path.join(perf_dir, "reports")
    os.makedirs(report_dir, exist_ok=True)

    print(f"🚀 성능 테스트 시작 (Host: {args.host}, Users: {args.users}, Time: {args.run_time})")

    # 2. Locust 부하 테스트 실행 (Headless 모드)
    # --csv 옵션을 통해 결과를 파일로 저장하여 Jenkins Performance Plugin에서 사용 가능하게 함
    csv_prefix = os.path.join(report_dir, f"result_{int(time.time())}")
    locust_cmd = (
        f"locust -f load/locust_test.py "
        f"--headless -u {args.users} -r {args.spawn_rate} "
        f"--run-time {args.run_time} --host {args.host} "
        f"--csv {csv_prefix} --only-summary"
    )

    exit_code = run_command(locust_cmd, cwd=perf_dir)

    if exit_code == 0:
        print(f"✅ 테스트 완료. 결과가 {report_dir} 에 저장되었습니다.")
    else:
        print(f"❌ 테스트 중 오류 발생 (Exit Code: {exit_code})")
        sys.exit(exit_code)

if __name__ == "__main__":
    main()
