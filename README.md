# crawler

설정 파일 정의를 통한 크롤러


# 크롤링 방식 
url 기반 필터링 크롤링
- a href 에 url 이 있다는 전제하에 모든 링크 수집 후, 필요한 URL과 중복 URL 을 필터링하여 크롤링함
- 중복 체크를 위해서 방문한 모든 URL 들을 비교 체크하기 때문에 해당 로직 오버로드 굉장히 심함
- 콘텐츠 파싱에 대한 부분은 html 을 지정하기 때문에 가장 종속적이지 않은 방식
- regex 활용

url 기반 시나리오 크롤링
- a href 에 url 이 있다는 전제하에 수집할 a 태그들을 지정하여 크롤링함
- 중복 체크 없어서 오버로드는 없지만 html 구조에 더 종속적임
- CSS selector 활용

action 기반 시나리오 크롤링
- 링크, 콘텐츠 수집 시 자바 스크립트, ajax 에 의한 비동기 로딩 방식일때 사용하는 selenium 기반 크롤링
- 웹브라우저의 ui 를 조작한다는 측면에서 action(버튼 클릭, 스크롤, 입력, 뒤로가기, 탭 이동 등) 단위로 설정함
- html 구조에 굉장히 종속적임 ui 조작의 모든 부분을 설정함
- CSS selector 활용

# 콘텐츠 수집 방식
설정 파일에 저장된 css selector 에서 element를 찾아서 수집함

# 저장 방식
설정 파일에 정의한 방식에 따라 가능
- CSV 파일 저장 : collect_recode와 collect_recode.element의 데이터 이름에 의해서 파일명, 헤더 자동 생성
- DB 저장 : collect_recode와 collect_recode.element의 데이터 타입에 의해서 테이블, 컬럼 자동 생성
