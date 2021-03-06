import React, { useState, useEffect } from 'react';
import classes from './App.module.css';
import axios from 'axios';
import { useHistory, useParams } from "react-router-dom";
import Button from 'react-bootstrap/Button';
import authHeader from "../../../service/auth-header";
import ReactLoading from 'react-loading';

const App = props => {
  let history = useHistory();
  let { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [question, setQuestion] = useState(null);
  const [userAnswer, setUserAnswer] = useState({id: null, progress: null, answers: [], questionId: null});
  const [pulse, setPulse] = useState(null);
  const [testStatus, setTestStatus] = useState(null);
  const [nextQuestion, setNextQuestion] = useState(null)

  const getPulse = () => {
    axios.get("http://localhost:8080/api/pulse",
      {headers: authHeader()}
    ).then(res => {
      if (res.data.error) {
        alert(res.data.error)
      } else {
        setPulse(res.data);
      }
    }).catch(err => console.log(err));
  }

  const changeUserAnswer = (id, progress, text, questionId) => {
    const newAnswer = !userAnswer?.answers.some(el => el.id === id);

    setUserAnswer(userAnswer => {
      const answers = newAnswer ? [...userAnswer.answers, {id, text}] : userAnswer.answers.filter(a => a.id !== id)
      return {...userAnswer, id: questionId, progress: progress, answers}
    })
  }

  useEffect(() => {
    axios.get("http://localhost:8080/api/test/" + id,
      {headers: authHeader()}
    ).then(res => {
      if (res.data.error) {
        alert(res.data.error)
      } else {
        setQuestion(res.data);
        setLoading(false);
        console.log(question)
      }
    }).catch(err => {
      console.log(err);
      alert(err.message)
    });
    // const interval = setInterval(() => {
    //   getPulse();
    // }, 2000);
    // return () => clearInterval(interval);
  }, []);

  const submit = () => {
    
    setLoading(true);
    axios.post("http://localhost:8080/api/test", userAnswer)
      .then(res => {
        if (res.data.error) {
          console.log(question)
          alert(res.data.error)
        } else {
          if (res.data.testStatus.includes('passed')) {
            history.replace('/test_results/' + res.data.progress);
            console.log(question)
          } else {
            
            setTestStatus(res.data.testStatus == 'true');
            setNextQuestion(res.data);
            console.log(question)
            setLoading(false);

          }
        }
      }).catch((e) => {
        setLoading(false);
        console.log(e);
      })
    setUserAnswer({id: null, progress: null, answers: []});
  }

  const goToNextQuestion = () => {
    console.log(nextQuestion)
    setQuestion(nextQuestion)
    setTestStatus(null);
    setNextQuestion(null);
  }

  return (
    <div>
      {console.log(question)}
      {(!loading) ?
      <div className={
        testStatus === true ?
          classes.UserTestCorrect :
          testStatus === null ?
            classes.UserTest :
            classes.UserTestWrong
      }>
        <h1>{question.text}</h1>
        <hr />
          <div>
            {question.answers.map(answer =>
              { return (
                <div
                  key={answer.id}
                  className={classes.Answer}
                >
                  <input
                    name="answer"
                    type="checkbox"
                    value={answer.text}
                    id={`radioButton${answer.id}`}
                    onClick={() => changeUserAnswer(answer.id, question.progress, answer.text, question.id)} />
                  <label htmlFor={`radioButton${answer.id}`}>
                    {" " + answer.text}
                  </label><br />
                </div>
              )}
            )}
            { nextQuestion === null ? 
              <Button onClick={submit} disabled={!userAnswer?.answers.length > 0}>Submit</Button> :
              <Button className={classes.ButtonGoToNextQuestion} onClick={goToNextQuestion} >Go Next</Button> 

            }
            
            {/* <p>{pulse}</p> */}
          </div>
      </div> :
      <div className={classes.Loading}>
        <ReactLoading type={"spinningBubbles"} color="#000000" />
      </div>}
    </div>
  )
}

export default App;
