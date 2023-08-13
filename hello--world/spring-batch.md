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