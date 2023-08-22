# 배치
- 배치처리란 상호작용이나 중단없이 유한한 데이터를 처리하는 것.
- 사용성, 확장성, 가용성, 보안

# 스프링 배치 프레임워크
## 스프링 배치 아키텍처
- 레이어 구조로 조립된 세 개의 티어
- 가장 바깥쪽에 **애플리케이션 레이어.**
- -> 배치 처리 구축에 사용되는 모든 사용자 코드 및 구성이 이 레이어에 포함.
- -> 업무로직, 서비스, 잡 구조화 관련 구성까지도 애플리케이션 레이어에 포함.
- -> 어플리케이션 레이어는 다음 레이어인 코어 레이어와 상호작용 하는데 대부분의 시간을 소비함.
- **코어레이어**에는 배치 도메인을 정의하는 모든 부분이 포함됨
- -> 코어 컴포넌트 요소에는 잡 및 스텝 인터페이스와, 잡 실행에 사용되는 인터페이스(JobLauncher, JobParameters 등)이 있음.
- *인프라스트럭처 레이어는* 가장 밑에 위치.
- -> 어떤 처리를 수행하려면 파일, 데이터베이스 등으로부터 읽고 쓸 수 있어야함,
- -> 잡 수행에 실패한 이후 재시도 될때 어떤 일을 수행할지를 다룰 수 있어야함.
- -> 공통 인프라스트럭처로 간주되며, 프레임워크의 인프라스트럭처 컴포넌트 내에 들어있음.

### cf) 스케줄러
- 일반적으로 스프링 배치가 스케줄러이거나 스케줄러를 가지고 있다고 오해하지만 그렇지 않음.
- 프레임워크 내에는 주어진 시간에 또는 주어진 이벤트에 따라 잡이 실행되도록 하는 스케줄링 기능 없음.
- -> 잡을 구동시키는 방법은 4장에서.

## 스프링으로 잡 정의하기.
### 잡
- 잡은 중단이나 상호작용 없이 처음부터 끝까지 실행되는 처리.
- 잡은 여러개의 스텝이 모여 이뤄질 수 있음
- 각 스텝이는 관련된 입출력이 있을 수 있음. 
- 스텝이 실패했을 때 반복 실행할 수도 있고 못할 수도 있다.
- 잡의 플로우는 조건부일 수 있음.

```java
import java.beans.BeanProperty;

public class exam {
    @Bean
    public AccountTasklet accountTasklet() {
        return new AccountTasklet();
    }

    @Bean
    public Job accountJob(){
        Step accountStop = 
                this.setpBuilderFactory
                        .get("accountStep")
                        .tasklet(accountTaskleft())
                        .build();
        
        return this.jobBuilderFactory
                .get("accountJob")
                .start("accountStep")
                .build();
    }
}
```
- 첫 번째 빈은 AccountTasklet.
- AccountTasklet은 커스텀 컴포넌트로써, 스텝이 동작하는 동안에 비즈니스 로직을 수행함.
- 스프링 배치는 AccountTasklet이 완료될 때까지 단일 메서드를 반복해서 호출하는데, 
- 이때 각각은 새 트랜잭션으로 호출됨.
- 두번째 빈은 실제 스프링 배치 잡.
- 이 빈 정의 내에서는 팩토리가 제공하는 빌더를 사용해
- -> 조금전 정의했던 AccountTasklet을 감싸는 스텝 하나를 생성.
- 그런 다음에 잡 빌더를 사용해 스텝을 감싸는 잡을 생성.
- 스프링 부트는 애플리케이션 기동시 이 잡을 찾아내 자동으로 실행시킴.

# 2장 - 스프링 배치
## 배치 아키텍처
- 애플리케이션 레이어
- 코어 레이어
- 인프라스트럭처 레이어 

## 잡과 스텝 
### 잡
- 자바나 xml을 사용해 구성된 **배치 잡**은 
- -> **상태를 수집하고 이전 상태에서 다음 상태로 전환**된다.
### 스텝
- 일반적으로 상태를 보여주는 단위는 스텝
- **스텝**은 잡을 구성하는 독립된 작업의 단위
- **태스크릿** 기반 스텝과 **청크** 기반 스텝
#### 태스크릿 기반 스텝
- 태스크릿 기반 스텝이 더 간단함
- Tasklet을 구현하면 되는데, 스텝이 중지될떄까지
- -> execute 메서드가 계속 반속해서 수행됨(execute호출때마다 독립적인 트랜잭션)
- 초기화, 저장 프로시저 실행, 알림 전송 등과 같은 잡에서 일반적으로 사용됨
#### 청크 기반 스텝
- 구조가 약간 더 복잡하며 Item 기반의 처리에 사용함
- ItemReader, ItemProcessor, ItemWriter라는 3개의 주요 부분
- ItemProcessor는 필수 아님

### 구조의 장점
- 각 스텝이 독립적으로 처리될 수 있도록 분리함
- 스텝은 자신에게 필요한 데이터를 가져와 필요한 업무 로직을 수행하고 적절한 위치에 데이터를 기록.
- 이처럼 스텝을 분리함으로써 많은 기능을 제공할 수 있음.
#### 유연성
- 스프링 배치는 재사용 가능하게 구성하도록 여러 빌더 클래스 제공
#### 유지보수성
- 각 스텝은 독립적임
- 스텝은 스프링 빈이므로 재사용 가능.
#### 확장성
- 스텝은 확장 가능한 병렬 실행 등의 다양한 방법을 제공
#### 신뢰성
- 여러 단계에 적용할 수 있는 오류처리 방법 제공. 재시도, 건너뛰기 등

## 잡 실행
- 잡이 실행될 때 스프링 배치의 많은 컴포넌트는 탄력성을 제공하기 위해 서로 상호작용함
### JobRepository
- 스프링 배치 아키텍처 내에 공유되는 주요 컴포넌트인 JobRepository
- 다양한 배치 수행과 수치 데이터뿐만 아니라 잡의 상태를 유지. 
- 수치데이터(== 시작시간, 종료시간, 상태, 읽기/쓰기 횟수 등)
- 일반적으로 관계형 데이터베이스를 사용하며 스프링 배치 내의 대부분 주요 컴포넌트가 공유함.
### JobLauncher 
- JobLauncher는 잡을 실행하는 역할을 담당
- Job.execute 메서드를 호출하는 역할 이외에도
- -> 잡의 재실행 가능 여부, 잡의 실행 방법, 파라미터 유효성 검증 등의 처리를 수행.
- JobLauncher가 이러한 처리 중에 어떤 수행을 할지 개발자가 구현하기에 달라짐
- **스프링 부트 환경이라면 스프링 부트가 즉시 잡을 시작하는 기능을 제공하므로 일반적으로 다룰 필요가 없음**
### 잡 실행 내부 순서
- 잡을 실행하면 해당 잡은 각 스텝을 실행
- 각 스탭이 실행되면 JobRepository는 현재 상태로 갱신됨. 
- 즉, **실행된 스텝, 현재 상태, 읽은 아이템 및 처리된 아이템 수 등이 모두 JobRepository에 저장됨**.
### 잡과 스텝의 유사한 처리방식 처리방식
- 잡과 스텝의 처리 방식은 매우 유사함.
- 잡은 구성된 스텝 목록에 따라 각 스텝을 실행함.
- -> 여러 아이템으로 이뤄진 청크의 처리가 스탭 내에서 완료될 때,
- -> 스프링 배치는 JopRepository내에 있는 JobExcution 또는 StepExecution을 현재 상태로 갱신.
- -> 스텝은 ItemReader가 읽은 아이템의 목록을 따라감
- -> 스텝이 각 청크를 처리할 때마다, JobRepository내 StepExecution의 스텝 상태가 업데이트됨.
- -> 현재까지의 커밋 수, 시작 및 종료 시간, 기타 다른 정보등이 JobRepository에 저장됨
- -> 잡 또는 스텝이 완료되면 JobRepository내에 있는 JobExecution 또는 StepExecution이 최종 상태로 업데이트됨.

#### JobExecution 과 StepExecution
- Job -> JobInstance -> JobExecution
- **JobInstance는 스프링 배치잡의 논리적인 실행**임(logical execution).
- JobInstance는 "잡의 이름"과 "잡의 논리적 실행을 위해 제공되는 고유한 식별 파라미터 모음"으로 유일하게 존재
- **JobExecution은 스프링 배치 잡의 실제 실행**을 의미.
- -> 잡을 구동할 떄마다 매번 새로운 JobExecution을 얻게 됨.
- ex) JobInstance를 얻지 못하는 예시
- 잡을 처음 실행하면 새로운 JobInstance 및 JobExecution을 얻음.
- -> 실행에 실패한 이후 다시 실행하면, 
- -> 해당 실행은 여전히 동일한 논리적 실행이므로(파라미터가 도잉ㄹ) 
- -> 새 JobInstance를 얻지 못함.
- -> 그대신 두번째 실행을 추적하기 위한 새로운 JobExecution을 얻음
- -> **JobInstance는 여러 개의 JobExecution을 가질 수 있음.**
- **StepExecution은 스텝의 실제 실행을 나타냄**
- -> StepInstance는 없음.
- -> 일반적으로 JobExecution을 여러개의 StepExecution과 연관됨.

## 병렬화 (5가지 방법)
- 다중 스레드 스텝을 통한 작업 분할
- 전체 스텝의 병렬 실행
- 비동기 ItemProcessor/ItemWriter 구성
- 원격 청킹
- 파티셔닝

### 다중 스레드 스텝
- 다중 스레드 스텝을 이용해 잡을 나누기
- 잡은 **청크라는 블록단위로 처리**되도록 구성
- -> 각 청크는 각자 독립적인 트랜잭션으로 처리됨.
- -> 일반적을 각 청크는 연속해서 처리됨
- -> **각 청크를 병렬로 처리**할 수 있음

### 병럴 스텝
- 스텝을 병렬로 실행하기
- ex) 입력 파일의 데이터를 읽어오는 스텝과 DB 저장 스텝 있을 때
- -> 스텝간에 서로 직접적인 관련이 없으므로 병럴 처리 가능

### 비동기 ItemProcessor/ItemWriter
- 계산 과정이 복잡한 경우 ItemProcessor에 병목현상 발생 가능
- AsynchronousItemProcessor는 ItemProcessor 호출 결과 반환하는 대신
- -> 각 호출에 대해 java.utilconcurrent.Future를 반환
- -> Future 목록은 AsynchronousItemWriter로 전달됨
- -> AsynchronousItemWriter로은 Future를 이용해 실제 결과를 얻어낸 후 이를 위임하는 ItemWriter에 전달

### 원격 청킹 (처리에 비해 I/O 적은 경우 )
- 여러 JVM에 처리를 분산. 
- 첫번째 원격 처리 방식은 원격 청킹
- -> 입력은 마스터 노드에서 표준 ItemReader를 사용해 이뤄짐
- -> 메시지 브로커 등을 통해 메시지 기반 POJO로 구성된 원격 워커 ItemProcessor로 전송됨
- -> 처리가 완료되면 워커는 업데이트된 아이템을 다시 마스터로 보내거나 직접 기록.

### 파티셔닝
- 스프링 배치는 원격 파티셔닝(마스터 및 원격 워커 사용) 및 
- -> 로컬 파티셔닝(워커의 스레드 사용) 모두 지원.
- 원격 파티셔닝과 원격 청킹의 두가지 주요 차이점은 
- -> 원격 파티셔닝을 사용하면 메시지 브로커 같은 통신 방법이 필요하지 않으며
- -> 마스터는 워커의 스텝 수집을 위한 컨트롤러 역할만 한다는 것. 
- -> 각 워커의 스텝은 독립적으로 동작하며 로컬로 배포된 것처럼 동일하게 구성됨.
- -> 차이점은 워크의 스텝이 자신의 잡 대신 마스터 노드로부터 일을 전달받는다는점.

# 배치 프로젝트

```java
@EnableBatchProcessing
@SpringBootApplication
public class HelloWorldApplication {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Step step(){
		return this.stepBuilderFactory.get("step1")
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

						System.out.println("Hello, World!");
						return RepeatStatus.FINISHED;
					}
				}).build();
	}

	@Bean
	public Job job(){
		return this.jobBuilderFactory.get("job")
				.start(step())
				.build();
	}
    
	public static void main(String[] args) {
		SpringApplication.run(HelloWorldApplication.class, args);
	}
}
```
## 추가된 세가지 주요부분
- @EnableBatchProcessing 애너테이션
- JobBuilderFactory
- StepBuilderFactory
- 스텝 및 잡 정의 

### @EnableBatchProcessing
- 배치 인프라스트럭처를 부트스트랩 하는 데 사용
- 다음과 같은 컴포넌트를 제공
- JobRepository : 실행 중인 잡의 상태를 기록하는 데 사용됨
- JobLauncher : 잡을 구동하는 데 사용됨
- JobExplorer : JobRepository를 사용해 읽기 전용 작업을 수행
- JobRegistry : 특정한 런처 구현체를 사용할 때 잡을 찾는 용도
- PlatformTransactionManager : 잡 진행 과정에서 트랜잭션을 다루는데 사용됨.
- JobBuilderFactory : 잡을 생성하는 빌더
- StepBuilderFactory : 스텝을 생성하는 빌더

### @SpringBootApplication
- @ComponentScan과 @EnableAutoConfiguration을 결합한 메타 애너테이션
- -> DataSource(데이터 소스) 뿐만 아니라 스프링 부트 기반 적절한 자동 구성 만듦.
#### DataSource  
- 배치에는 DataSource가 제공되어야하는 요구사항 있음 (@SpringBootApplication이 자동 구성)
- JobRepository와 PlatformTransactionManager는 필요에 따라 데이터 소스 사용
- 스프링 부트는 클래스 패스에 존재하는 DBMS 정보 사용해 이를 처리.
- -> 구동 시 DBMS 감지해 내장 데이터 소스 생성.

### @EnableBatchProcessing
- @EnableBatchProcessing를 적용하므로써
- -> 스프링 배치가 제공하는 잡 빌더, 스텝 빌더를 자동 주입함
- JobBuilderFactory, StepBuilderFactory

### 잡, 스텝 만들기 
- 위 작업들 다음으로 스텝을 만듬
- 잡 :  단일 스텝으로 구성되므로 간단하게 스텝이름만 지정함
- 스텝 : 이름과 태스크릿 필요. -> 인라인으로 작성된 태스크릿은 잡에서 실제 일을 수행
- -> 예제에선 hello world 출력 후, RepeatStatus.FINISHED 반환
- -> RepeatStatus.FINISHED 반환한다는 것은 태스크릿이 완료되었음을 배치에 알리는 것
- -> RepeatStatus.CONTINUABLE 반환 가능 -> 태스크릿 다시 호출
- 스텝 구성 후 이 스텝을 이용해 잡을 작성 가능. -> JobBuilderFactory 이용해서 잡 구성.
- -> 잡 이름과 해당 잡에서 시작할 스텝을 구성.

### 잡 실행하기 
- 기본적으로 스프링 부트는 구성된 ApplicationContext내에서 찾은 모든 잡을 구동시에 실행함.
#### 실행 시 내부 작동
- 스프링 부트의 JobLauncherCommandLineRunner 
- 이 컴포넌트는 스프링 배치가 클래스 경로에 있다면 실행 시 로딩됨
- JobLauncher를 사용해 ApplicationContext에서 찾아낸 모든 잡을 실행.
- -> 이후 잡은 첫번째 스텝을 실행
- -> 이때 트랜잭션이 시작됨
- -> Tasklet이 실행되고 결과과 JobRepository에 갱신

# 예제 잡 애플리케이션 
## 은행 거래명세서 예제
- 매일밤 수행되는 배치처리는 거래 정보를 사용해 거래명세서를 생성
- 기존 계좌에 거래내역을 적용한 후 각 계좌별로 거래명세서를 생성
### 다양한 입출력 방식
- 은행 거래명세서 잡은 일반적인 텍스트 파일인 플랫파일과 데이터베이스에서 입력을 받음
- 또한 플랫 파일과 데이터베이스에 출력함
- -> 다양한 Reader와 Writer를 사용
### 오류 처리
- 로깅, 레코드 건너뛰기, 로직 재시도 등
### 확장성
- 배치 처리 튜닝


## 배치 잡 설계하기
### 잡의 처리 흐름
- 다음과 같은 4개의 스텝을 가진 잡 하나 만들기
- 고객 데이터 가져오기 -> 거래정보 데이터 가져오기 -> 현재잔액 계산하기 -> 월별고객 거래명세서 생성하기

#### 스텝1-고객 데이터 가져오기
- 스프링 배치는 여러 레코드 형식을 처리할 수 있는 기능을 제공
- 쓰기처리할 때 오류 최소화 하기위해 ItemProcessor 사용하여 유효성 검증
- ItemWriter 구현체 사용해 레코드 유형에 따라 적절하게 데이터를 갱신

#### 스텝2-거래 정보 데이터 가져오기 
- 스프링 배치는 강력한 ItemReader 및 ItemWriter 구현체 제공하므로
- -> 이 스텝에선 XML을 읽은 뒤 DB에 기록하는 구현체 사용

#### 현재 잔액 계산하기
- 계좌 테이블에 잔액 갱신
- -> 드라이빙 쿼리 패턴으로 각 계좌 내 거래 레코드를 순서대로 가져와서
- -> 해당 거래 레코드가 현재 잔액에 미치는 영향 계산
- -> 후 계좌 테이블의 잔액을 갱신

#### 월별 고객 거래명세서 생성
- 이 스텝에서는 고객의 거래명세서를 포함하는 인쇄 파일을 계좌 하나씩 생성
- 먼저 ItemReader를 사용해 DB에 고객 데이터를 읽어오기
- ItemProcessor에게 해당 고객 데이터 보내기
- 최종 아이템을 파일기반 ItemWriter에게 전달. 

### 데이터 모델 이해하기
- Customer: 고객별로 고객 이름 및 연락처 정보 비롯한 모든 고객정보
- Account : 모든 고객 계좌 정보
- CustomerAccount : 조인테이블 - 다대다 관계 해결
- Transaction : 모든 거래정보 저장.

# 잡과 스텝 이해하기
## 잡 소개하기
- 유일함 : 스프링 빈
- 순서를 가진 여러 스텝의 목록임 : 스텝의 순서가 중요함
- 처음부터 끝까지 실행 가능 : 외부 의존성 없이 실행될 수 있는 일련의 스텝.
- -> ex) 파일이 수신되기를 세번째 스텝이 기다리도록 구성x, 대신 파일이 도착했을 떄 잡을 시작.
- 독립적 : 의존성을 관리할 수 있어야함.
- -> 잡의 실행은 스케줄러와 같은 것이 책임지지만
- -> 잡은 자신이 처리하기로 정의된 모든 요소를 제어할 수 있음

## 잡의 생명주기
- 잡의 실행은 잡 러너(job runner)에서 시작됨
- 잡 러너는 잡 이름과 여러 파라미터를 받아들여 잡을 실행시키는 역할을 함.
### 잡러너 세가지
- **CommandLineJobRunner** : 스크립트나 명령행에서 직접 잡을 실행할 때 사용-
- -> 스프링을 부트스트랩하고 전달받은 파라미터를 사용해 요청된 잡을 실행
- **JobRegistryBackgroundJobRunner** : 스프링을 부트스트랩해서 기동한 자바 프로세스 내에서
- -> 쿼츠나 JMX후크 같은 스케줄러를 사용해 잡을 실행한다면,
- -> 스프링이 부트스트랩될 때 실행 가능한 잡을 갖고 있는 JobRegistry를 생성함.
- **JobLauncherCommandLineRunner** : 스프링 부트는 이것을 이용해 잡 시작
- -> 별고의 구성이 없다면 ApplicationContext에 정의된 Job 타입의 모든 빈을 기동시에 실행함.
### 잡러너 추가내용
- 잡러너는 프레임워크가 제공하는 표준 모듈 아님
- 각 시나리오마다 서로 다른 구현체 필요하므로 JobRunner 인터페이스 제공하지 않음
- 실제 실행할 떄 진입점은 잡러너가 아닌 
- -> org.springframework.batch.core.launch.JobLauncher 인터페이스의 구현체
#### JobLauncher
- 스프링 배치는 단일 JobLauncher만 제공
- org.springframework.batch.core.launch.support.SimpleJobLauncher
- CommandLineJobRunner와 JobLauncherCommandLineRunner 내부에 사용
- -> JobLauncher는 요청된 잡을 실행할 때 코어의 스프링 TaskExecutor 인터페이스를 사용
- -> SyncTaskExecutor 사용하면 JobLauncher와 동일 스레드에서 실행됨
![img](/img/img.png)
### 잡, 잡인스턴스, 잡익스큐션의 관계
- 배치 잡이 실행되면 JobInstance가 생성됨
- JobInstance는 잡의 논리적 실행을 나타내며, 두가지로 식별됨
- 하나는 잡 이름
- 하나는 잡에 전달되 실행 시에 사용되는 식별 파라미터.
- 잡의 실행과 잡의 실행시도는 다른 개념
#### Job, JobInstance 관계 예시
- 매일 실행될 것으로 예상되는 잡이 있을 때
- 잡 구성은 한 번만 됨
- 매일 새로운 파라미터를 잡에게 전달해 실행함으로써 새로운 JobInstance 얻음
- 각 JobInstance는 성공적으로 완료된 JobExecution이 있다면 완료된 것으로 간주됨. 
- cf) JobInstance
- JobInstance는 한 번 성공적으로 완료되면 다시 실행시킬 수 없음
- -> JonInstance는 잡 이름과, 전달된 식별 파라미터로 식별되므로
- -> 동일한 식별 파라미터 사용하는 잡은 한 번만 실행 가능
#### JobInstance 상태 식별 방법
- 스프링 배치가 JobInstance 상태 알아내는 방법
- JobRepository가 사용하는 BATCH_JOB_INSTANCE 테이블
- -> 나머지 테이블은 이 테이블을 기반으로 파생됨
- -> JonInstance를 식별할 때는 
- -> BATCH_JOB_INSTANCE와 BATCH_JOB_EXECUTION_PARAMS 테이블을 사용
- BATCH_JOB_INSTANCE.JOB_KEY의 실체는 잡 이름과 식별 파라미터의 해시 값.

#### JobExecution
- JobExecution은 잡 실행의 실제 시도를 의미.
- 잡이 처음부터 끝까지 단 번에 실행 완료 됐다면
- -> 해당 JobInstance와 JobExecution은 단 하나씩 존재
- -> 첫번째 잡 실행 후 오류 상태로 종료됐다면, 해당 JobInstance를 실행하려고 시도할 때마다 새로운 JobExecution이 생성됨 
- -> 이때 JobInstance에는 동일한 식별 파라미터가 전달됨
- 스프링 배치가 잡을 실행할 때 생성하는 각 JobExecution은 
- -> BATCH_JOB_EXECUTION 테이블의 레코드로 저장됨
- -> 또 JobExecution이 실행될 때 상태는 BATCH_JOB_EXECUTION_CONTEXT 테이블에 저장됨
- -> 잡에서 오류가 발생하면 스프링 배치는 이 정보를 이용해 올바른 지점에서부터 다시 잡을 시작함.

## 잡 구성하기
### 잡의 기본 구성

```java
@EnableBatchProcessing //배치 잡 수행에 필요한 인프라스트럭처 제공
@SpringBootApplication
public class DemoApplication {
    // 잡 빌더
    @Autowired 
    private JobBuilderFactory jobBuilderFactory; 

    //스텝 빌더
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // 잡 빈 정의->jobBuilderFactory->jobBuilder->잡구성
    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
                .start(step1())
                .build();
    }

    //스텝 빈 정의 -> stepBuilderFactory->stepBuilder -> 스텝 정의
    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println("Hello, World");
                    return RepeatStatus.FINISHED;
                })).build();
    }
}
```
- @EnableBatchProcessing -> 배치 인프라스트럭처 제공
- JobBuilderFactory, StepBuilderFactory -> 자동 와이어링
- 잡 빈 정의->jobBuilderFactory->jobBuilder->잡구성
- 스텝 빈 정의 -> stepBuilderFactory->stepBuilder -> 스텝 정의

# 잡 파라미터
- JobInstance가 잡 이름 및 잡에 전달된 식별 파라미터로 식별됨
- 동일한 식별 파라미터를 사용해 동일한 잡을 두 번 이상 실행할 수 없음
- -> 다시 실행하면 JobInstanceAlreadyCompleteException을 전달받음

## 잡에 파라미터 전달하는 방법
- 스프링 배치는 잡에 파라미터를 전달할 수 있게 해줄 뿐만아니라,
- 잡 실행 전에 파라미터를 자동으로 증가시키거나, 검증할 수도 있게 해줌.
- 잡에 파라미터 전달 방법은 **사용자가 잡을 어떻게 호출하는지**에 따라 달라짐
- **잡 러너**의 기능 중 하나가 잡 실행에 필요한 
- -> **JobParameters객체를 생성**해 JobInstance에 전달하는 것.
- -> 명령행에서 잡을 시작할 떄와, 쿼츠 스케줄러에서 잡을 시작할 때의 파라미터 전달 방식이 다르기 때문
- JobLauncherCommandLineRunner를 기준으로 알아보기. 
### JobLauncherCommandLineRunner로 파라미터 전달 방법
```
java -jar demo.jar name=Michael
```
- -> 위에서 name이라는 파라미터 하나 전달함
- -> 사용자가 배치 잡에서 파라미터를 전달하면 **잡러너는 JobParameters 인스턴스를 생성**하는데,
- 해당 인스턴스는 잡이 전달받는 모든 **파라미터 컨테이너의 역할**을 함.
- cf) 스프링부트 명령행 기능 사용해 프로퍼티 구성과 다르므로, --으로 전달하면 안됨. 시스템 프로퍼티와도 다르므로 -D 아규먼트도 X

### JobParameters
- JobParameters는 java.util.Map<String, JobParameter> 객체의 래퍼에 불과함.
- 스프링 배치는 파라미터의 타입 변환 기능을 제공. 변환된 타입에 맞게 JobParameter의 접근자를 제공
- 타입변환기능 사용하라면 파라미터 이름 뒤에 괄호쓰고 그 안에 파라미터의 타입 명시(소문자)
- -> 잡에 전달한 파라미터를 확인하고 싶다면 JobRepository를 살펴보면 됨.
- -> JobRepository의 데이터베이스 스키마에는 BATCH_JOB_EXECUTION_PARAMS 테이블이 있음.

### 식별되지 않는 파라미터
- 식별에 사용되지 않는 파라미터도 있음
- 특접 잡 파라미터가 식별에 사용되지 않게 하려면 접두사 "-"를 사용
```
java -jar demo.jar executionData(date)=2020/12/27 -name=Michael
```
- 위의 name은 식별에 사용되지 않는 파라미터이므로
- -> 실패한 후, -name=John으로 변경하더라도 기존 JobInstance를 기반으로 JobExecution 생성


#### 잡파라미터에 접근하기
- 잡 파라미터에 접근하는 방법은 접근 위치에 따라 몇 가지 방식 있음
```
@Bean
public Step step1() {
    return this.stepBuilderFactory.get("step1")
            .tasklet(((contribution, chunkContext) -> {
                System.out.println("Hello, World");
                return RepeatStatus.FINISHED;
            })).build();
}
```
##### ChunckContext
- ChunckContext : HelloWorld 태스크릿을 보면 execute 메서드가
- 두 개의 파라미터를 전달 받는 것을 볼 수 있다. 
- 첫번째 파라미터인 StepContribution은 아직 커밋되지 않은 현재 **트랜잭션에 대한 정보**를 갖고 있음
- 두번째 파라미터인 ChunkContext는 실행 시점의 **잡 상태**를 제공. 
- 또한 태스크릿 내에서는 처리중인 **청크와 관련된 정보**도 갖고 있음
- -> 해당 청크 정보는 스텝 및 잡과 관련된 정보도 갖고 있음. 
- -> ChunkContext에는 JobParameters가 포함된 StepContext의 참조가 있음.
```
@Bean
public Tasklet helloWorldTasklet(){
    return (contribution, chunkContext)->{
        String name = (String) chunkContext.getStepContext()
                .getJobParameters() // JobParameters로 접근 !!!
                .get("name");

        System.out.println(String.format("Hello, %s!", name));
        return RepeatStatus.FINISHED;
    };
}
```
- 스프링 배치는 JobParameter 클래스의 인스턴스에 잡 파라미터를 저장하는데, getJobParameters()를 호출하는 방식으로
- -> 잡 파라미터를 가져오면 Map<String, Object>가 반환됨. -> 타입 캐스팅 필요

###### 늦은 바인딩
- 늦은 바인딩 : 스텝이나 잡을 제외한 프레임워크 내 특정 부분에 파라미터를 전달 하는 방법은
- 스프링 구성을 사용해 주입.
- -> Jobparameters는 변경할 수 없으므로 부트스트랩 시 바인딩 하는 것이 좋음
```
@StepScope //늦은 바인딩 허용
@Bean
public Tasklet helloWorldTasklet(
        @Value("#{jobParameters['name']}") String name){
    
    return (contribution, chunkContext) -> {
        System.out.println(String.format("Hello, %s!", name));
        return RepeatStatus.FINISHED;
    };
}
```
- 위처럼 스텝 스코프나 잡 스코프를 사용하면 스텝이나 잡의 실행범위에 들어갈때까지 빈 생성응ㄹ 지연
- -> 이렇게 함으로써 명령행 또는 다른소스에서 받아들인 잡 파라미터를 빈 생성시점에 주입.

### 파라미터 특화기능
### 잡 파라미터 유효성 검증
- JobParametersValidator 인터페이스를 구현하여 잡 내에 구성하기
```java
public class ParameterValidator implements JobParametersValidator {
    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        String fileName = parameters.getString("fileName");

        if(!StringUtils.hasText(fileName)){
            throw new JobParametersInvalidException("fileName parameter is missing");
        } else if (!StringUtils.endsWithIgnoreCase(fileName, "csv")) {
            throw new JobParametersInvalidException("fileName parameter does " +
                    "not use the csv file extension");
        }
    }
}
```
- 반환타입이 void 이므로 JobParametersInvalidException이 발생하지 않는다면 
- 유효성 검증이 통과했다고 판단
- 위 처럼 직접 구현할 수도 있지만, 필수 파라미터 누락없이 전달됐는지 유효성 검증기인 
- -> DefaultJobParametersValidator를 기본적으로 제공.
- requiredKeys와 optionalKeys라는 선택적 의존성 있음

```java
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.context.annotation.Bean;

class ex {
    @Bean
    public JobParametersValidator validator() {
        DefaultJobParametersValidator validator = new DefaultJobParametersValidator();
        
        validator.setRequiredKeys(new String[] {"fileName"}); //필수 파라미터 목록
        validator.setOptionalKeys(new String[] {"name"}); // 필수 아닌 파라미터 목록
        return validator;
    }
}
```
- -> 위에서 fileName이 필수 파라미터로 구성되어 있으므로
- -> fileName을 잡 파라미터로 전달하지 않고 잡을 실행하려고 하면 유효성 검증에 실패함
- -> 위처럼 설정하면 이 잡에 전달할 수 있는 파라미터는 fileName, name
- -> **이 외의 파라미터가 변수에 전달되면 유효성 검증에 실패(옵션키 없어도 통과)**.
- -> 옵션 키가 구성되어 있지 않고, 필수키만 구성되어 있다면, 필수만 전달하고 그 외에 다른것을 전달해도 통과
- -> (옵션키 있으면 설정된 파라미터들 외의 전달하면 실패)

##### 유효성 검증기 적용하기
- 두개의 유효성 검증기 사용하고 싶지만, 
- JobBuilder의 메소드는 하나의 JobParameterValidator 인스턴스만 지정하게 돼 있음.
- -> CompositeJobParametersValidator 사용.
```java
class ex {
    @Bean
    public CompositeJobParametersValidator validator() {
        CompositeJobParametersValidator validator =
                new CompositeJobParametersValidator();


        DefaultJobParametersValidator defaultJobParametersValidator =
                new DefaultJobParametersValidator(
                        new String[]{"fileName"},
                        new String[]{"name"});

        defaultJobParametersValidator.afterPropertiesSet();

        // 복합 검증 설정
        validator.setValidators(
                Arrays.asList(new ParameterValidator(),
                        defaultJobParametersValidator));

        return validator;
    }


    // 잡 빈 정의->jobBuilderFactory->jobBuilder->잡구성
    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
                .start(step1())
                .validator(validator())
                .build();
    }

    //스텝 빈 정의 -> stepBuilderFactory->stepBuilder -> 스텝 정의
    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .tasklet(helloWorldTasklet(null, null)).build();
    }

    @StepScope
    @Bean
    public Tasklet helloWorldTasklet(
            @Value("#{jobParameters['name']}") String name,
            @Value("#{jobParameters['filaName']}") String fileName) {

        return (contribution, chunkContext) -> {
            System.out.println(String.format("Hello, %s!", name));
            System.out.println(String.format("fileName, %s!", fileName));
            return RepeatStatus.FINISHED;
        };
    }
}
```
- -> filaName만 보내면 통과 !

### 잡 파라미터 증가시키기
- 위에선 주어진 식별 파라미터로 잡을 단 한 번만 실행하는 제약이 있었음.
- -> JobParametersIncrementer 사용하여 잡을 여러번 실행 시키기
- JobParametersIncrementer는 잡에서 사용할 파라미터를 고유하게 생성할 수 있도록 스프링 배치가 제공하는 인터페이스
- 
