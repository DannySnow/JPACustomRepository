package com.wiseyep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiseyep.CustomRepository.CustomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.List;

@SpringBootApplication
@RestController
public class JpaSpecificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpaSpecificationApplication.class, args);
	}

	@Autowired
	DemoService demoService;


	@RequestMapping(method = RequestMethod.GET,value = "/teachers")
	public String queryTeachers(@RequestParam String teacherName,@RequestParam String sex,@RequestParam String studentName) throws JsonProcessingException {
		return demoService.queryTeacher(teacherName,sex,studentName);
	}
}



@Component
class DemoService{

	@Autowired
	DemoMapper demoMapper;

	public String queryTeacher(String teacherName, String sex, final String studentName) throws JsonProcessingException {
		Page<Teacher> teachers = demoMapper.findBySpecification()
				.like(null != teacherName,"name","%" + teacherName + "%")
				.and(null != studentName, "name", new Specification() {
					@Override
					public Predicate toPredicate(Root root, CriteriaQuery criteriaQuery, CriteriaBuilder criteriaBuilder) {
						Predicate predicate = criteriaBuilder.conjunction();
						predicate.getExpressions().add(criteriaBuilder.equal(root.join("students", JoinType.INNER).get("name"),studentName));
						return predicate;
					}
				})
				.equal(null != sex,"sex",sex)
				.build();
		return new ObjectMapper().writeValueAsString(teachers.getContent());
	}
}


interface DemoMapper extends CustomRepository<Teacher,Long>{

}


@Entity
class Teacher{
	@Id
	@GeneratedValue
	private long id;

	private String name;

	private String sex;

	@OneToMany(mappedBy = "teacher")
	List<Student> students;
}


@Entity
class Student{
	@Id
	@GeneratedValue
	private long id;

	private String name;

	private String sex;

	@ManyToOne
	Teacher teacher;
}